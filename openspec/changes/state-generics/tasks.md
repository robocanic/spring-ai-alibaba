## 1. 新增基础类型（零风险，无破坏性）

- [ ] 1.1 新增 `GraphState` 标记接口（`com.alibaba.cloud.ai.graph.GraphState`）
- [ ] 1.2 新增 `@StateField` 注解（`com.alibaba.cloud.ai.graph.annotation.StateField`）
- [ ] 1.3 新增 `NodeActionResult<S>` 类（`com.alibaba.cloud.ai.graph.action.NodeActionResult`）
- [ ] 1.4 新增 `StateFieldScanner` 工具类（`com.alibaba.cloud.ai.graph.utils.StateFieldScanner`）
  - [ ] 1.4.1 实现 `getKeyStrategies(Class<S>)` 方法
  - [ ] 1.4.2 实现 `toMap(S state)` 方法（null 字段跳过，transient 字段跳过）
  - [ ] 1.4.3 实现 `fromMap(Map, Class<S>)` 方法（反射注入，无参构造器创建实例）
  - [ ] 1.4.4 实现 `diff(S snapshot, S current)` 方法（ReplaceStrategy 比较，AppendStrategy subList）
  - [ ] 1.4.5 实现 `getStreamingFieldKey(Class<S>)` 方法
- [ ] 1.5 新增 `OverallStateNodeAction` 兼容 Shim 接口（含 `wrap()` 静态方法）

## 2. 向后兼容基础改造

- [ ] 2.1 `OverAllState` 实现 `GraphState` 接口（仅添加 `implements GraphState`，无其他改动）
- [ ] 2.2 为 `StateGraph` 新增 `StateGraph(Class<S> stateClass)` 构造器（内部调用 `StateFieldScanner.getKeyStrategies` 并执行运行时校验）
- [ ] 2.3 为 `StateGraph` 新增 `StateGraph(String name, Class<S> stateClass)` 构造器
- [ ] 2.4 运行时校验逻辑实现（在新构造器中校验 `@StateField` 使用合法性，违规抛 `GraphStateException`）
  - [ ] 2.4.1 `streaming=true` 字段唯一性校验
  - [ ] 2.4.2 `AppendStrategy` 字段必须是 `List` 类型校验
  - [ ] 2.4.3 `fieldName` 不重复校验
  - [ ] 2.4.4 `transient` 字段不得标注 `@StateField` 校验
  - [ ] 2.4.5 State 类必须有 public 无参构造器校验

## 3. Action 接口泛型化

- [ ] 3.1 `NodeAction<S extends GraphState>` 泛型化（返回类型改为 `NodeActionResult<S>`）
- [ ] 3.2 `AsyncNodeAction<S extends GraphState>` 泛型化（返回类型改为 `CompletableFuture<NodeActionResult<S>>`）
- [ ] 3.3 `AsyncNodeActionWithConfig<S extends GraphState>` 泛型化
- [ ] 3.4 `NodeActionWithConfig<S extends GraphState>` 泛型化
- [ ] 3.5 `EdgeAction<S extends GraphState>` 泛型化
- [ ] 3.6 `AsyncEdgeAction<S extends GraphState>` 泛型化
- [ ] 3.7 `AsyncEdgeActionWithConfig<S extends GraphState>` 泛型化
- [ ] 3.8 `CommandAction<S extends GraphState>` 泛型化
- [ ] 3.9 `MultiCommandAction<S extends GraphState>` 泛型化
- [ ] 3.10 `InterruptableAction<S extends GraphState>` 泛型化（`interruptAfter` 参数改为 `NodeActionResult<S>`）
- [ ] 3.11 `Command<S extends GraphState>` record 泛型化（`update: S`，允许 null）
- [ ] 3.12 `MultiCommand<S extends GraphState>` record 泛型化（`update: S`，允许 null）

## 4. StateSerializer<S> 泛型化

- [ ] 4.1 `StateSerializer<S extends GraphState>` 抽象类泛型化
- [ ] 4.2 新增抽象方法 `fromMap(Map<String, Object> data)` 与 `toMap(S state)`
- [ ] 4.3 新增 `cloneState(S state)` 默认实现（`fromMap(toMap(state))`）
- [ ] 4.4 适配 `SpringAIJacksonStateSerializer<S>` 实现 `fromMap` / `toMap`
  - 对 `OverAllState`：`toMap` 返回 `state.data()`，`fromMap` 调用原 stateFactory
  - 对 POJO：`toMap` 调用 `StateFieldScanner.toMap`，`fromMap` 调用 `StateFieldScanner.fromMap`
- [ ] 4.5 适配 `SpringAIStateSerializer<S>` 同上
- [ ] 4.6 `ObjectStreamStateSerializer<S>` 泛型化适配

## 5. 输出类型泛型化

- [ ] 5.1 `NodeOutput<S extends GraphState>` 泛型化（`state` 字段类型改为 `S`，`state()` 返回 `S`）
- [ ] 5.2 `StreamingOutput<T, S extends GraphState>` 双泛型化（继承自 `NodeOutput<S>`）
- [ ] 5.3 适配 `StreamingOutput` 的所有构造器（`OverAllState` 参数改为 `S`）

## 6. StateGraph<S> 与 CompiledGraph<S> 核心泛型化

- [ ] 6.1 `StateGraph<S extends GraphState>` 泛型化（类声明及内部 node/edge 存储结构）
- [ ] 6.2 `StateGraph.addNode()` / `addEdge()` 等方法适配新版泛型 Action 接口
- [ ] 6.3 `CompiledGraph<S extends GraphState>` 泛型化
- [ ] 6.4 `CompiledGraph.stream(S initialState)` 新增重载（内部调用 `toMap` 转换）
- [ ] 6.5 `CompiledGraph.stream()` 返回类型更新为 `Flux<GraphResponse<NodeOutput<S>>>`
- [ ] 6.6 节点执行流程适配（在调用 NodeAction 前做 `fromMap` + `cloneState`，调用后做 `diff` + `updateState`）
- [ ] 6.7 流式节点执行适配（改为通过 `result.hasStreamingFlux()` 判断流式路径，弃用 Map value 中的 Flux 检测）
- [ ] 6.8 `Command<S>.update` 处理逻辑（null 跳过；非 null 调用 `toMap` 跳过 null 字段）
- [ ] 6.9 `StateGraph` 新增 `StateGraph(String name, Class<S>, StateSerializer<S>)` 构造器

## 7. spring-ai-alibaba-starter-builtin-nodes 适配

- [ ] 7.1 盘点所有内置节点的 `NodeAction` 实现类
- [ ] 7.2 将各节点返回值由 `Map<String, Object>` 改为 `NodeActionResult<OverAllState>`
- [ ] 7.3 验证各节点编译通过且单元测试通过

## 8. spring-ai-alibaba-agent-framework 适配

- [ ] 8.1 盘点 `ReactAgent` 及相关类中所有直接使用 `NodeAction` / `AsyncNodeActionWithConfig` 的位置
- [ ] 8.2 适配内部节点/边 Action 实现（入参改为 `OverAllState`，返回 `NodeActionResult<OverAllState>`）
- [ ] 8.3 适配 `AgentToSubCompiledGraphNodeAdapter`（泛型参数适配）
- [ ] 8.4 验证 agent-framework 编译通过且相关测试通过

## 9. 集成验证

- [ ] 9.1 编写 POJO 状态端到端集成测试（`ChatState` 含 AppendStrategy 字段 + 流式输出节点）
- [ ] 9.2 编写向后兼容测试（旧版 `OverAllState` + `KeyStrategyFactory` 代码原样运行）
- [ ] 9.3 编写父子图异构 State 集成测试（父图 `ChatState`，子图 `OverAllState`）
- [ ] 9.4 编写 `OverallStateNodeAction.wrap()` 兼容测试
- [ ] 9.5 编写 `@StateField` 约束校验的单元测试（各校验项逐一覆盖）
- [ ] 9.6 编写 `StateFieldScanner.diff` 的单元测试（ReplaceStrategy / AppendStrategy 各场景）
- [ ] 9.7 全量回归：确保现有测试套件全部通过
