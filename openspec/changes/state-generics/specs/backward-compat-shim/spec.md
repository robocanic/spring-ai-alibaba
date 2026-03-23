## ADDED Requirements

### Requirement: OverAllState 实现 GraphState
系统 SHALL 使 `OverAllState` 实现 `GraphState` 接口，成为框架的默认状态类型，确保所有现有基于 `OverAllState` 的代码无需修改即可兼容新版泛型 API。

#### Scenario: 旧版 StateGraph 代码无需修改
- **WHEN** 用户使用旧版 `new StateGraph(keyStrategyFactory)` 构造（内部隐式为 `StateGraph<OverAllState>`）
- **THEN** 编译通过，运行行为与改造前完全相同

#### Scenario: OverAllState 可作为 GraphState 类型参数
- **WHEN** 代码中声明 `StateGraph<OverAllState>` 或 `NodeAction<OverAllState>`
- **THEN** 编译通过，`OverAllState` 满足 `S extends GraphState` 约束

---

### Requirement: OverallStateNodeAction 兼容 Shim
系统 SHALL 提供 `OverallStateNodeAction` 函数式接口，保留旧版返回 `Map<String, Object>` 的 Action 签名，并提供 `wrap()` 静态方法将其包装为 `NodeAction<OverAllState>`。

#### Scenario: 旧版 Action 通过 wrap 接入新版 API
- **WHEN** 用户将旧版 `OverallStateNodeAction` 通过 `OverallStateNodeAction.wrap(action)` 包装
- **THEN** 返回合法的 `NodeAction<OverAllState>`，可直接传入 `StateGraph.addNode()`

#### Scenario: wrap 内部执行旧版逻辑
- **WHEN** 框架调用 wrapped `NodeAction<OverAllState>` 时
- **THEN** 框架将 `OverAllState` 传入旧版 action，取得 `Map<String, Object>` partial update，调用 `state.updateState(partialUpdate)`，然后封装为 `NodeActionResult.of(state)` 返回
