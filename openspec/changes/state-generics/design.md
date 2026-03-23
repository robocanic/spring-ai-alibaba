# State 泛型化 Design

## Context

`spring-ai-alibaba-graph-core` 是一个图工作流引擎，以 `OverAllState`（`Map<String, Object>` 封装）为核心状态容器。它被 `ReactAgent`、`StateGraph`、`CompiledGraph`、所有内置节点以及上层 agent-framework 广泛依赖。

当前状态管理在以下边界上强依赖 `OverAllState`：
- `NodeAction.apply(OverAllState) → Map<String, Object>`
- `AsyncNodeAction` / `EdgeAction` / `CommandAction` 等接口的入参
- `NodeOutput.state()` 返回值
- `StateSerializer` 读写对象
- `GraphRunnerContext` 内部持有

目标是引入 POJO 状态类支持，同时不破坏现有大量基于 `OverAllState` 的代码。

---

## Goals / Non-Goals

**Goals:**
- 用户可以用普通 Java POJO 定义 Graph 状态，享受类型安全与 IDE 支持
- `OverAllState` 的所有现有用法无需修改即可编译运行（向后兼容）
- 框架在边界处自动完成 POJO ↔ `Map<String, Object>` 转换
- 支持字段级更新策略声明（`@StateField(strategy = ...)`）
- 支持流式输出字段标记（`@StateField(streaming = true)`）
- 父子图可以使用不同 State 类型

**Non-Goals:**
- 不改造 `GraphRunnerContext` 等框架内部执行逻辑（内部 Map 不变）
- 不引入 Annotation Processor（本阶段仅做运行时校验）
- 不支持 POJO 字段中间插入或删除（`AppendStrategy` 只支持末尾追加）
- 不对 `OverAllState` 的内部数据结构做任何修改

---

## Decisions

### 决策 1：API 层泛型化，内部 Map 不变

**选项 A（本方案）**：只在用户 API 边界泛型化，`GraphRunnerContext` 内部仍持有 `OverAllState`，在调用节点前/后由 `StateFieldScanner` 做 POJO ↔ Map 转换。

**选项 B**：整个内部执行链路全部泛型化（`GraphRunnerContext<S>`、`Checkpoint<S>` 等）。

**选择 A，理由：**
- 内部执行链路（`GraphRunnerContext`、`Checkpoint`、`Store`）涉及文件数量多、耦合深，全量泛型化改动量是方案 A 的 5-10 倍，风险极高
- `Map<String, Object>` 是父子图状态传递的自然中间格式，保留 Map 路径使父子图异构 State 天然可行
- 方案 A 对用户体验的提升与方案 B 等效，差异对用户不可见

---

### 决策 2：由 `StateSerializer<S>` 负责深拷贝

节点执行前需要对当前 State 做快照，用于 diff 计算（尤其是 `AppendStrategy` 字段）。

**选项 A（本方案）**：`StateSerializer.cloneState(S)` 默认实现为 `fromMap(toMap(state))`，即 POJO → Map → POJO 的往返转换，利用已有的 Map 序列化能力。

**选项 B**：直接使用 Java 对象序列化（`ObjectOutputStream` / `ObjectInputStream`）做深拷贝。

**选择 A，理由：**
- `StateSerializer` 已是框架的序列化抽象层，深拷贝是其天然职责延伸
- `toMap` 本身会跳过 `transient` 字段，深拷贝结果与序列化存储语义一致
- 方案 B 要求 POJO 中所有字段都可 Java 序列化，限制更多（如 `Flux` 字段）

---

### 决策 3：AppendStrategy 字段使用 diff 机制

对于 `@StateField(strategy = AppendStrategy.class)` 字段，框架计算增量：
`delta = newList.subList(snapshot.size(), currentList.size())`

**选项 A（本方案）**：节点调用前快照，返回后取末尾新增部分作为 delta，再通过 `AppendStrategy.apply(stored, delta)` 合并。

**选项 B**：用户调用时只往 POJO 字段里放新增的元素（框架直接用返回值做 append）。

**选择 A，理由：**
- 选项 B 违反 POJO 直觉——用户自然地 `state.getMessages().add(msg)` 然后 return，而不是构造一个只含新增元素的 POJO
- 选项 A 对用户完全透明，用户只需按直觉操作 POJO，框架处理 diff 细节

**约束（需在文档中说明）**：`AppendStrategy` 字段只支持末尾追加，不支持中间插入或删除。

---

### 决策 4：`Command<S>.update` 为 null 时表示不更新

**选项 A（本方案）**：`Command<S>` 完全泛型化，`update` 字段类型为 `S`（可为 null）。`update != null` 时，框架调用 `StateFieldScanner.toMap(update)` 跳过 null 字段，直接 `updateState`（无需 diff 快照）。

**选项 B**：`Command.update` 保持 `Map<String, Object>`，只有 `CommandAction<S>` 入参泛型化。

**选择 A，理由：**
- 与原则"所有用户侧 API 都用泛型"保持一致
- `Command` 的更新语义是"显式 partial 修改"，用户主动构造只填需要修改字段的 POJO，null 字段自然表示"不改这个字段"，无需 diff

---

### 决策 5：运行时校验优先，Annotation Processor 延后

**选项 A（本方案）**：在 `StateGraph(Class<S>)` 构造时，通过反射扫描 POJO 做合法性校验，违规抛 `GraphStateException`。

**选项 B**：同时引入 Annotation Processor 在编译期检测。

**选择 A，理由：**
- Annotation Processor 需独立模块、自定义 javac 插件注册，额外工作量约 3-4 天
- 运行时在 `new StateGraph<>(...)` 时立即失败，对用户也足够友好
- 方案 A 的校验逻辑后续可直接迁移到 AP，不重复劳动

---

### 决策 6：`stream()` 同时支持 `Map` 和 `S` 两种入参

```java
compiled.stream(Map.of("input", "你好"))  // 旧用法，向后兼容
compiled.stream(myInitialState)           // 新用法，类型安全
```

当传入 `S` 时，框架调用 `stateSerializer.toMap(initialState)` 转为 Map，再走原有初始化路径。无需改动内部执行逻辑。

---

## Risks / Trade-offs

| 风险 | 缓解方案 |
|------|----------|
| `AppendStrategy` diff 假设末尾追加，用户若在中间插入会产生错误 delta | 运行时校验 + 文档中明确约束 |
| `toMap/fromMap` 往返转换有性能开销（每个节点调用两次） | 对大多数 Graph 场景（节点数 < 100，State 字段数 < 20）开销可忽略；高频场景用户可自定义 `StateSerializer<S>` 覆盖 `cloneState` 实现 |
| `Command<S>.update` 中 null 字段被跳过，用户无法将字段显式置为 null | 文档中说明此限制；需要置 null 的场景使用 `OverallStateNodeAction.wrap()` 回退到 Map 模式 |
| `StateSerializer<S>` 抽象类改动是 public API，自定义序列化器的用户需升级 | 提供默认实现覆盖新增方法，降低迁移成本；旧的 `SpringAIJacksonStateSerializer` 等保持同名，内部适配 |
| `NodeOutput<S>` / `StreamingOutput<T,S>` 泛型化后使用者侧泛型签名变长 | 主要影响 Agent 侧消费代码；agent-framework 内部可使用通配符 `NodeOutput<?>` 降低噪音 |
| 父子图 Map key 对不上时静默忽略 | 运行时 debug log 中打印未匹配的 key；后续可加可选的严格模式 |

---

## Migration Plan

1. 在 `graph-core` 中新增 `GraphState`、`@StateField`、`StateFieldScanner`、`NodeActionResult`（无破坏性变更）
2. `OverAllState` 实现 `GraphState`（无破坏性变更）
3. 泛型化所有用户 API 接口（`NodeAction<S>` 等），同步添加 `OverallStateNodeAction` shim（此步会使现有代码产生未检查警告，但可通过 `@SuppressWarnings` 或 shim 消除）
4. 泛型化 `StateGraph<S>` / `CompiledGraph<S>`（保留旧构造器，旧代码无需修改）
5. 泛型化 `StateSerializer<S>`（旧实现类提供新方法的默认 OverAllState 实现）
6. 泛型化 `NodeOutput<S>` / `StreamingOutput<T,S>`
7. 适配 `builtin-nodes` 和 `agent-framework`

**回滚策略**：各步骤独立，任一步骤出问题可单独回滚，不影响前序步骤。

---

## Open Questions

| 问题 | 当前状态 |
|------|---------|
| 父子图 key 不匹配时是否需要严格模式（抛异常而非静默忽略）？ | 暂定静默忽略 + debug log，后续再决定是否加严格模式 |
| `AppendStrategy` 是否需要支持非 List 的 Collection（如 Set）？ | 当前 diff 机制依赖 `List.subList`，Set 不支持；暂不支持非 List 的 AppendStrategy 字段 |
| `StateFieldScanner` 是否需要支持父类字段继承扫描？ | 暂时只扫描声明类本身的字段，不扫描父类；后续根据需求扩展 |
