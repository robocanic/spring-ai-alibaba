## ADDED Requirements

### Requirement: GraphState 标记接口
系统 SHALL 提供 `GraphState` 标记接口，作为所有自定义状态类的基础契约。

#### Scenario: 用户定义自定义状态类
- **WHEN** 用户创建一个 Java 类并 `implements GraphState`
- **THEN** 该类可作为 `StateGraph<S>` 的类型参数 S 使用

#### Scenario: GraphState 要求 Serializable
- **WHEN** 用户定义的 GraphState 实现类未实现 `Serializable`
- **THEN** 编译器报错（因为 `GraphState extends Serializable`）

---

### Requirement: @StateField 注解
系统 SHALL 提供 `@StateField` 注解，允许用户在 POJO 字段上声明更新策略、key 映射名以及流式标记。

#### Scenario: 未标注字段使用默认 ReplaceStrategy
- **WHEN** POJO 字段未标注 `@StateField`
- **THEN** 框架对该字段使用 `ReplaceStrategy`

#### Scenario: 指定自定义更新策略
- **WHEN** 字段标注 `@StateField(strategy = AppendStrategy.class)`
- **THEN** 框架对该字段使用 `AppendStrategy` 进行合并

#### Scenario: 指定 fieldName 映射
- **WHEN** 字段标注 `@StateField(fieldName = "input")`
- **THEN** 框架在 POJO ↔ Map 转换时使用 `"input"` 作为 Map key，而非 Java 字段名

#### Scenario: 标记流式字段
- **WHEN** 字段标注 `@StateField(streaming = true)`
- **THEN** 框架将该字段 key 视为流式输出的目标写入字段

#### Scenario: transient 字段不参与 state 管理
- **WHEN** POJO 字段声明为 `transient`
- **THEN** 框架在 `toMap` 时跳过该字段，在 `fromMap` 时也不注入该字段

---

### Requirement: @StateField 约束校验
系统 SHALL 在 `StateGraph(Class<S>)` 构造时对 POJO 的 `@StateField` 使用进行合法性校验，违规抛出 `GraphStateException`。

#### Scenario: streaming=true 字段唯一性
- **WHEN** 同一 State 类中有多个字段标注 `@StateField(streaming = true)`
- **THEN** 抛出 `GraphStateException`，消息包含 State 类名

#### Scenario: AppendStrategy 字段类型约束
- **WHEN** 字段标注 `@StateField(strategy = AppendStrategy.class)` 但字段类型不是 `List`
- **THEN** 抛出 `GraphStateException`，消息包含字段名

#### Scenario: fieldName 不重复
- **WHEN** 同一 State 类中两个字段声明了相同的 `@StateField(fieldName = "x")`
- **THEN** 抛出 `GraphStateException`，消息包含冲突的 fieldName

#### Scenario: transient 字段不得标注 @StateField
- **WHEN** `transient` 字段同时标注了 `@StateField`
- **THEN** 抛出 `GraphStateException`，消息包含字段名

#### Scenario: State 类必须有无参构造器
- **WHEN** 传入的 POJO 类没有 public 无参构造器
- **THEN** 抛出 `GraphStateException`，消息包含类名
