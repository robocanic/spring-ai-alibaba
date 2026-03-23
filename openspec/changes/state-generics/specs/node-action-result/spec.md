## ADDED Requirements

### Requirement: NodeActionResult 非流式返回
系统 SHALL 提供 `NodeActionResult<S>` 作为所有 `NodeAction<S>` 的统一返回类型，非流式场景使用 `NodeActionResult.of(state)` 构造。

#### Scenario: 节点返回非流式结果
- **WHEN** 节点调用 `NodeActionResult.of(state)`
- **THEN** 框架取 `result.state()` 做 diff 并更新内部状态

#### Scenario: hasStreamingFlux 为 false
- **WHEN** 通过 `NodeActionResult.of(state)` 构造
- **THEN** `result.hasStreamingFlux()` 返回 `false`，`result.streamingFlux()` 返回 null

---

### Requirement: NodeActionResult 流式返回
系统 SHALL 支持节点通过 `NodeActionResult.ofStreaming(state, flux)` 同时携带状态更新和流式 Flux。

#### Scenario: 节点返回流式结果
- **WHEN** 节点调用 `NodeActionResult.ofStreaming(state, chatFlux)`
- **THEN** `result.hasStreamingFlux()` 返回 `true`，`result.streamingFlux()` 返回 `chatFlux`

#### Scenario: 框架检测到流式 Flux 后走流式路径
- **WHEN** `result.hasStreamingFlux()` 为 `true`
- **THEN** 框架通过 `StateFieldScanner.getStreamingFieldKey` 确定目标字段 key，并将 Flux 输出内容写入该字段

#### Scenario: 流式完成后状态字段更新
- **WHEN** Flux 流式输出完成
- **THEN** 框架将流式输出的完整内容更新到 `streaming=true` 对应字段的 State 中

---

### Requirement: 替代旧版 Flux 嵌入 Map 的流式方式
系统 SHALL 通过 `NodeActionResult.hasStreamingFlux()` 判断流式路径，替代原有在 `Map<String, Object>` 中检测 `Flux<?>` value 的方式。

#### Scenario: 旧版 Map 中嵌入 Flux 的写法不再支持（针对新泛型 NodeAction）
- **WHEN** 用户使用 `NodeAction<S>` 接口（新版）
- **THEN** 流式输出 MUST 通过 `NodeActionResult.ofStreaming` 声明，框架不再检测 Map value 中的 Flux 类型
