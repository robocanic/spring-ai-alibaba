# State 泛型化 Proposal

## 背景与动机

当前 `spring-ai-alibaba-graph-core` 中的状态管理完全依赖 `OverAllState`，它是一个基于 `Map<String, Object>` 的弱类型容器。用户在节点中读写状态时必须使用字符串 key 和手动类型转换（如 `state.value("messages", List.class)`），存在以下问题：

1. **缺乏类型安全**：编译期无法检测 key 拼写错误或类型不匹配，运行时才会暴露
2. **可读性差**：每个节点无法知道某一个 key 在哪被写入、在哪被读取
3. **IDE 支持弱**：无法利用自动补全、重构、跳转定义等 IDE 能力

目标：允许用户定义普通 Java POJO 作为 Graph 的状态类型，通过字段声明和注解描述状态结构与更新策略，同时保持与现有 `OverAllState` API 的完全向后兼容。

---

## 架构策略

**API 层泛型化，内部 Map 不变。**

- 面向用户的所有 API（`NodeAction`、`EdgeAction`、`StateGraph`、`CompiledGraph` 等）引入泛型参数 `<S extends GraphState>`
- 框架内部（`GraphRunnerContext`、`OverAllState.updateState`、`KeyStrategy` 等）保持以 `Map<String, Object>` 运转
- 边界处由 `StateFieldScanner` 负责 POJO ↔ Map 的双向转换

```
用户代码 (ChatState POJO)
        │
        ▼ StateFieldScanner.fromMap / toMap
框架内部 (OverAllState + Map<String, Object>)
        │
        ▼ StateSerializer.write / read
Checkpoint / Store
```

---

## 新增核心 API

### 1. `GraphState` 标记接口

```java
public interface GraphState extends Serializable {}
```

- 所有自定义状态类必须实现此接口
- `OverAllState` 同时实现 `GraphState`，保证向后兼容
- `extends Serializable` 使框架可通过序列化实现状态深拷贝

### 2. `@StateField` 注解

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StateField {
    Class<? extends KeyStrategy> strategy() default ReplaceStrategy.class;
    String fieldName() default "";   // 为空则使用 Java 字段名
    boolean streaming() default false;
}
```

- `strategy`：该字段的更新策略，未标注默认 `ReplaceStrategy`
- `fieldName`：序列化时使用的 key 名，用于与旧版 `OverAllState` key 对齐
- `streaming`：标记该字段承载流式数据，**每个 State 类中最多一个字段可设为 true**
- `transient` 字段自动跳过，不参与 state 管理

### 3. `StateFieldScanner` 工具类

负责扫描 POJO 的 `@StateField` 注解，提供以下能力：

| 方法 | 说明 |
|------|------|
| `getKeyStrategies(Class<S>)` | 提取字段到 `KeyStrategy` 的映射 |
| `toMap(S state)` | POJO → `Map<String, Object>`（null 字段跳过） |
| `fromMap(Map, Class<S>)` | `Map<String, Object>` → POJO（反射注入） |
| `diff(S snapshot, S current)` | 提取两个 POJO 间的变化 delta Map |
| `getStreamingFieldKey(Class<S>)` | 获取 `streaming=true` 字段的 key |

**`diff` 规则：**
- `ReplaceStrategy` 字段：新值与快照不同则加入 delta
- `AppendStrategy` 字段：`newList.subList(snapshot.size, new.size)`（仅提取末尾新增部分，要求 AppendStrategy 字段只做末尾追加）

### 4. `NodeActionResult<S>`

```java
public class NodeActionResult<S extends GraphState> {
    // 非流式
    public static <S extends GraphState> NodeActionResult<S> of(S state);
    // 流式
    public static <S extends GraphState> NodeActionResult<S> ofStreaming(S state, Flux<?> flux);

    public S state();
    public Flux<?> streamingFlux();
    public boolean hasStreamingFlux();
}
```

取代原先 `NodeAction` 返回 `Map<String, Object>` 和在 Map value 中放 `Flux` 的方式。

---

## 核心 API 泛型化

所有面向用户的 API 均引入 `<S extends GraphState>` 类型参数：

```java
// Action 接口
@FunctionalInterface
public interface NodeAction<S extends GraphState> {
    NodeActionResult<S> apply(S state) throws Exception;
}

@FunctionalInterface
public interface AsyncNodeAction<S extends GraphState> {
    CompletableFuture<NodeActionResult<S>> apply(S state);
}

public interface AsyncNodeActionWithConfig<S extends GraphState> {
    CompletableFuture<NodeActionResult<S>> apply(S state, RunnableConfig config);
}

public interface NodeActionWithConfig<S extends GraphState> {
    NodeActionResult<S> apply(S state, RunnableConfig config) throws Exception;
}

@FunctionalInterface
public interface EdgeAction<S extends GraphState> {
    String apply(S state) throws Exception;
}

public interface AsyncEdgeAction<S extends GraphState> {
    CompletableFuture<String> apply(S state);
}

public interface AsyncEdgeActionWithConfig<S extends GraphState> {
    CompletableFuture<String> apply(S state, RunnableConfig config);
}

@FunctionalInterface
public interface CommandAction<S extends GraphState> {
    Command<S> apply(S state) throws Exception;
}

@FunctionalInterface
public interface MultiCommandAction<S extends GraphState> {
    MultiCommand<S> apply(S state) throws Exception;
}

public interface InterruptableAction<S extends GraphState> {
    Optional<InterruptionMetadata> interrupt(String nodeId, S state, RunnableConfig config);
    Optional<InterruptionMetadata> interruptAfter(String nodeId, S state,
        NodeActionResult<S> actionResult, RunnableConfig config);
}
```

### `Command<S>` 与 `MultiCommand<S>` 泛型化

```java
public record Command<S extends GraphState>(String gotoNode, S update) {
    // update 允许为 null，表示只路由不修改状态
    public Command(String gotoNode) {
        this(gotoNode, null);
    }
}

public record MultiCommand<S extends GraphState>(List<String> gotoNodes, S update) {
    public MultiCommand(List<String> gotoNodes) {
        this(gotoNodes, null);
    }
}
```

`Command<S>.update` 的处理规则：
- `null`：仅路由，不更新状态
- 非 `null`：`StateFieldScanner.toMap(update)` 跳过 null 字段，得到 partial Map，直接调用 `updateState`（无需 diff，因为用户显式构造 partial POJO）

### `StateGraph<S>` 与 `CompiledGraph<S>`

```java
public class StateGraph<S extends GraphState> {
    // 新增：POJO 状态，KeyStrategy 从 @StateField 自动提取
    public StateGraph(Class<S> stateClass) { ... }
    public StateGraph(String name, Class<S> stateClass) { ... }
    public StateGraph(String name, Class<S> stateClass, StateSerializer<S> stateSerializer) { ... }

    // 保留：原有构造器，S 默认绑定 OverAllState，向后兼容
    public StateGraph(KeyStrategyFactory keyStrategyFactory) { ... }
    public StateGraph(String name, KeyStrategyFactory keyStrategyFactory) { ... }
    public StateGraph(String name, KeyStrategyFactory keyStrategyFactory, StateSerializer stateSerializer) { ... }
}

public class CompiledGraph<S extends GraphState> {
    // 两种入参形式
    public Flux<GraphResponse<NodeOutput<S>>> stream(Map<String, Object> inputs) { ... }
    public Flux<GraphResponse<NodeOutput<S>>> stream(S initialState) { ... }
}
```

### `NodeOutput<S>` 与 `StreamingOutput<T, S>`

```java
public class NodeOutput<S extends GraphState> {
    public S state() { return state; }
    // ... 其他字段不变（node, agent, tokenUsage, subGraph）
}

public class StreamingOutput<T, S extends GraphState> extends NodeOutput<S> {
    // 原有 T 泛型保留（originData 类型）
    // 新增 S 泛型（state 类型）
}
```

`GraphResponse` 本身已是泛型 `GraphResponse<E>`，只需将 `E` 指定为 `NodeOutput<S>` 即可，无需修改 `GraphResponse` 本身。

---

## `StateSerializer<S>` 泛型化与深拷贝

```java
public abstract class StateSerializer<S extends GraphState> {
    // 原有序列化/反序列化（用于 checkpoint）
    public abstract void write(S object, ObjectOutput out) throws IOException;
    public abstract S read(ObjectInput in) throws IOException, ClassNotFoundException;

    // 新增：POJO ↔ Map 转换（框架内部边界翻译）
    public abstract S fromMap(Map<String, Object> data);
    public abstract Map<String, Object> toMap(S state);

    // 深拷贝（节点执行前快照，默认实现为 fromMap(toMap(state))）
    public S cloneState(S state) {
        return fromMap(toMap(state));
    }
}
```

**两种默认实现：**

| 实现类 | 适用场景 |
|--------|----------|
| `SpringAIJacksonStateSerializer<S>` | POJO 状态（Jackson 序列化），`StateGraph(Class<S>)` 自动使用 |
| `SpringAIStateSerializer<OverAllState>` | 原 `OverAllState`（Java ObjectStream），保留现有行为 |

---

## 框架内部节点执行流程（伪代码）

```java
// CompiledGraph<S> 执行单个节点
private void executeNode(String nodeId, AsyncNodeActionWithConfig<S> action) {
    // 1. 从内部 Map 构造 POJO
    S currentState = stateSerializer.fromMap(internalOverAllState.data());

    // 2. 深拷贝作为 diff 基准
    S snapshot = stateSerializer.cloneState(currentState);

    // 3. 调用用户 NodeAction
    NodeActionResult<S> result = action.apply(currentState, config).get();

    // 4. Diff 提取 delta（ReplaceStrategy 取新值，AppendStrategy 取末尾新增）
    Map<String, Object> delta = scanner.diff(snapshot, result.state());

    // 5. 使用现有 OverAllState.updateState 合并（内部不变）
    internalOverAllState.updateState(delta);

    // 6. 流式处理（判断 streamingFlux != null 而非检测 Map 中的 Flux value）
    if (result.hasStreamingFlux()) {
        String fieldKey = scanner.getStreamingFieldKey(stateClass);
        handleStreaming(result.streamingFlux(), fieldKey, nodeId);
    }

    // 7. 构造 NodeOutput<S> 供消费者使用
    S outputState = stateSerializer.fromMap(internalOverAllState.data());
    emit(new NodeOutput<>(nodeId, outputState));
}
```

---

## 运行时校验（StateGraph 构造时）

`StateGraph(Class<S> stateClass)` 构造时进行如下校验，违反则抛出 `GraphStateException`：

| 校验项 | 错误信息示例 |
|--------|-------------|
| `streaming=true` 字段全局唯一 | `State class ChatState has multiple streaming fields` |
| `AppendStrategy` 字段必须是 Collection 类型 | `Field 'messages' uses AppendStrategy but is not a Collection` |
| `fieldName` 在同一 State 类中不重复 | `Duplicate fieldName 'input' in ChatState` |
| `transient` 字段不得标注 `@StateField` | `Transient field 'logger' cannot be annotated with @StateField` |
| State 类必须有无参构造器（fromMap 反射需要） | `GraphState class ChatState must have a no-arg constructor` |

后续可将上述逻辑迁移至 Annotation Processor 以实现编译期检测。

---

## 向后兼容

### `OverAllState` 实现 `GraphState`

```java
public final class OverAllState implements GraphState { ... }
```

### `OverallStateNodeAction` 兼容 Shim

保留旧版返回 `Map<String, Object>` 的 Action 签名：

```java
@FunctionalInterface
public interface OverallStateNodeAction {
    Map<String, Object> apply(OverAllState state) throws Exception;

    static NodeAction<OverAllState> wrap(OverallStateNodeAction action) {
        return state -> {
            Map<String, Object> partialUpdate = action.apply(state);
            state.updateState(partialUpdate);
            return NodeActionResult.of(state);
        };
    }
}
```

### 父子图异构 State 转换

父子图允许使用不同的 State 类型，以 Map 为桥梁：

```
父图 ChatState → toMap() → Map → 传入子图 → OverAllState(子图内部) → data() → Map → fromMap() → 更新父图 ChatState
```

子图返回的 Map key 须与父图 POJO 字段名（或 `@StateField.fieldName`）对应，否则静默忽略（建议在文档中说明此约束）。

---

## 效果对比

### 改造前

```java
KeyStrategyFactory keyStrategyFactory = () -> Map.of(
    "messages", new AppendStrategy(),
    "currentStep", new ReplaceStrategy(),
    "input", new ReplaceStrategy()
);

StateGraph stateGraph = new StateGraph(keyStrategyFactory);

stateGraph.addNode("chatNode", (OverAllState state) -> {
    String input = state.value("input", "");
    List<Message> messages = state.value("messages", List.class).orElse(new ArrayList<>());
    Message response = chatClient.call(input);
    return Map.of("messages", List.of(response), "currentStep", "completed");
});
```

### 改造后

```java
public class ChatState implements GraphState {
    @StateField(strategy = AppendStrategy.class)
    private List<Message> messages = new ArrayList<>();

    private String currentStep;   // 默认 ReplaceStrategy
    private String input;         // 默认 ReplaceStrategy

    transient Logger log = LoggerFactory.getLogger(ChatState.class); // 不参与序列化

    // getters / setters ...
}

StateGraph<ChatState> stateGraph = new StateGraph<>(ChatState.class);

stateGraph.addNode("chatNode", (ChatState state) -> {
    String input = state.getInput();
    Message response = chatClient.call(input);
    state.getMessages().add(response);      // IDE 自动补全，类型安全
    state.setCurrentStep("completed");
    return NodeActionResult.of(state);
});
```

---

## 变更范围

### `spring-ai-alibaba-graph-core`

**新增（低风险）：**
- `GraphState.java`
- `@StateField.java`
- `StateFieldScanner.java`
- `NodeActionResult.java`
- `OverallStateNodeAction.java`（兼容 shim）

**修改（中等风险）：**
- `NodeAction` / `AsyncNodeAction` / `AsyncNodeActionWithConfig` / `NodeActionWithConfig`
- `EdgeAction` / `AsyncEdgeAction` / `AsyncEdgeActionWithConfig`
- `CommandAction` / `MultiCommandAction` / `InterruptableAction`
- `Command` / `MultiCommand`（record 泛型化）
- `NodeOutput` / `StreamingOutput`
- `StateSerializer`（泛型化 + 新增 toMap/fromMap/cloneState）
- `StateGraph`（新增 POJO 构造器，保留原有构造器）
- `CompiledGraph`（泛型化，新增 POJO 入参的 stream()）
- `OverAllState`（实现 `GraphState`）

**内部不变：**
- `GraphRunnerContext`（仍持有 `OverAllState`，边界处做翻译）
- `KeyStrategy` / `AppendStrategy` / `ReplaceStrategy`
- Checkpoint / Store
- 所有内部 node/edge 实现类

### `spring-ai-alibaba-starter-builtin-nodes`（Breaking Change）

所有内置节点适配新版 `NodeAction<S>` 定义，返回 `NodeActionResult<S>`。

### `spring-ai-alibaba-agent-framework`

内部自定义节点/边适配新版接口定义，`ReactAgent` 等保持使用 `OverAllState`（通过兼容构造器），对外暴露的 API 按需泛型化。
