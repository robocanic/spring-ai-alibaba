## ADDED Requirements

### Requirement: 所有 Action 接口泛型化
系统 SHALL 将所有面向用户的 Action 接口引入 `<S extends GraphState>` 泛型参数，入参由 `OverAllState` 变为 `S`。

#### Scenario: NodeAction 使用 POJO 状态
- **WHEN** 用户定义 `NodeAction<ChatState>` 并实现 `apply(ChatState state)`
- **THEN** 框架在调用前将内部 Map 转换为 `ChatState` 实例并传入，调用后将返回的 `NodeActionResult<ChatState>` diff 并写回内部 Map

#### Scenario: EdgeAction 使用 POJO 状态
- **WHEN** 用户定义 `EdgeAction<ChatState>` 并实现 `apply(ChatState state) → String`
- **THEN** 框架调用前将内部 Map 转为 `ChatState` 实例传入，EdgeAction 只读状态做路由，返回下一节点名

#### Scenario: CommandAction 使用 POJO 状态
- **WHEN** 用户定义 `CommandAction<ChatState>` 并实现 `apply(ChatState state) → Command<ChatState>`
- **THEN** 框架调用前将内部 Map 转为 `ChatState` 传入，Command 的 update 字段经 `toMap`（跳过 null）后写入内部状态

---

### Requirement: Command<S> 完全泛型化
系统 SHALL 将 `Command` record 泛型化为 `Command<S extends GraphState>`，`update` 字段类型由 `Map<String, Object>` 变为 `S`（可为 null）。

#### Scenario: Command 只路由不更新状态
- **WHEN** 用户构造 `new Command<>("nextNode")`（update 为 null）
- **THEN** 框架路由到目标节点，不修改内部状态

#### Scenario: Command 携带 partial POJO 更新
- **WHEN** 用户构造 `new Command<>("nextNode", partialState)` 且 `partialState` 中只有部分字段非 null
- **THEN** 框架调用 `StateFieldScanner.toMap(partialState)` 跳过 null 字段，得到 partial Map 并执行 `updateState`

---

### Requirement: MultiCommand<S> 完全泛型化
系统 SHALL 将 `MultiCommand` record 泛型化为 `MultiCommand<S extends GraphState>`，语义与 `Command<S>` 一致。

#### Scenario: MultiCommand 并行路由
- **WHEN** 用户构造 `new MultiCommand<>(List.of("nodeA", "nodeB"), partialState)`
- **THEN** 框架并行执行 nodeA 和 nodeB，partial state update 逻辑与 Command 相同

---

### Requirement: StateGraph<S> 新增 POJO 构造器
系统 SHALL 为 `StateGraph` 新增接受 `Class<S>` 的构造器，自动从 `@StateField` 注解提取 `KeyStrategy` 映射，无需用户手动提供 `KeyStrategyFactory`。

#### Scenario: 使用 POJO 构造器创建 StateGraph
- **WHEN** 调用 `new StateGraph<>(ChatState.class)`
- **THEN** 框架自动扫描 `ChatState` 的 `@StateField`，构建 KeyStrategy 映射，并进行运行时校验

#### Scenario: 保留原有 KeyStrategyFactory 构造器
- **WHEN** 调用 `new StateGraph(keyStrategyFactory)`（旧代码）
- **THEN** 编译和运行正常，行为与改造前完全相同

---

### Requirement: CompiledGraph<S> 泛型化与双入参 stream()
系统 SHALL 将 `CompiledGraph` 泛型化为 `CompiledGraph<S extends GraphState>`，`stream()` 方法同时支持 `Map<String, Object>` 和 `S` 两种初始输入形式。

#### Scenario: 使用 Map 启动执行（向后兼容）
- **WHEN** 调用 `compiled.stream(Map.of("input", "你好"))`
- **THEN** 框架将 Map 直接作为初始输入，行为与改造前相同

#### Scenario: 使用 POJO 启动执行
- **WHEN** 调用 `compiled.stream(initialChatState)`
- **THEN** 框架调用 `stateSerializer.toMap(initialChatState)` 转为 Map，再走原有初始化路径

#### Scenario: stream() 返回类型包含泛型 state
- **WHEN** 订阅 `Flux<GraphResponse<NodeOutput<S>>>` 的输出
- **THEN** 每个 `NodeOutput` 的 `state()` 方法返回类型为 `S`（而非 `OverAllState`）

---

### Requirement: NodeOutput<S> 泛型化
系统 SHALL 将 `NodeOutput` 泛型化为 `NodeOutput<S extends GraphState>`，`state()` 返回类型由 `OverAllState` 变为 `S`。

#### Scenario: 消费者获取强类型 state
- **WHEN** 消费者从 `NodeOutput<ChatState>` 调用 `state()`
- **THEN** 返回 `ChatState` 实例，无需手动类型转换

---

### Requirement: StreamingOutput<T, S> 双泛型化
系统 SHALL 将 `StreamingOutput<T>` 扩展为 `StreamingOutput<T, S extends GraphState>`，继承自 `NodeOutput<S>`。

#### Scenario: 消费者从流式输出获取状态
- **WHEN** 消费者从 `StreamingOutput<ChatResponse, ChatState>` 调用 `state()`
- **THEN** 返回 `ChatState` 实例

---

### Requirement: StateSerializer<S> 泛型化及新增转换方法
系统 SHALL 将 `StateSerializer` 泛型化为 `StateSerializer<S extends GraphState>`，新增 `fromMap`、`toMap` 和 `cloneState` 方法。

#### Scenario: cloneState 深拷贝
- **WHEN** 调用 `stateSerializer.cloneState(currentState)` 于节点执行前
- **THEN** 返回与 `currentState` 值相同但引用独立的新 POJO 实例

#### Scenario: 旧版 OverAllState 序列化器保持原有行为
- **WHEN** 使用 `SpringAIStateSerializer<OverAllState>` 或 `SpringAIJacksonStateSerializer<OverAllState>`
- **THEN** `toMap(state)` 返回 `state.data()`，`fromMap(map)` 调用原有 stateFactory，行为不变
