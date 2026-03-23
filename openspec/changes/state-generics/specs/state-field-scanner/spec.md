## ADDED Requirements

### Requirement: StateFieldScanner 继承链字段收集
系统 SHALL 在所有扫描操作中沿继承链向上遍历（直至 `Object`），收集当前类及所有父类声明的字段，子类字段优先（子类与父类存在同名字段时，子类声明的字段覆盖父类）。

#### Scenario: 子类与父类字段均被收集
- **GIVEN** `ChildState extends ParentState implements GraphState`，父类声明字段 `query`，子类声明字段 `answer`
- **WHEN** 任意 `StateFieldScanner` 操作以 `ChildState.class` 为参数
- **THEN** `query` 和 `answer` 均参与扫描

#### Scenario: 子类字段覆盖父类同名字段
- **GIVEN** 子类与父类均声明了名为 `result` 的字段（子类添加了 `@StateField` 注解，父类未标注）
- **WHEN** 任意扫描操作处理 `result` 字段
- **THEN** 使用子类声明的字段定义（带 `@StateField` 的那个），父类同名字段被忽略

#### Scenario: 多级继承全链路收集
- **GIVEN** `C extends B extends A implements GraphState`，三级各自声明不同字段
- **WHEN** 以 `C.class` 为参数调用任意扫描操作
- **THEN** A、B、C 三级的字段全部被扫描到（同名字段以最接近子类的声明为准）

---

### Requirement: StateFieldScanner POJO 到 Map 转换
系统 SHALL 提供 `StateFieldScanner`，将 `GraphState` POJO 转换为 `Map<String, Object>`，跳过 null 字段和 transient 字段；扫描范围包含完整继承链（见上方继承链收集规约）。

#### Scenario: 正常字段转换
- **WHEN** 调用 `StateFieldScanner.toMap(state)` 且字段值非 null
- **THEN** 返回 Map 中包含该字段的 key（`fieldName` 或 Java 字段名）与对应值

#### Scenario: 父类字段也被包含在 toMap 结果中
- **WHEN** 调用 `StateFieldScanner.toMap(childState)`，且父类中声明了值非 null 的字段
- **THEN** 返回 Map 中包含父类字段对应的 key 与值

#### Scenario: null 字段跳过
- **WHEN** 调用 `StateFieldScanner.toMap(state)` 且某字段值为 null
- **THEN** 返回的 Map 中不包含该字段对应的 key

#### Scenario: transient 字段跳过
- **WHEN** POJO 中存在 `transient` 修饰的字段
- **THEN** `toMap` 结果中不包含该字段

---

### Requirement: StateFieldScanner Map 到 POJO 转换
系统 SHALL 提供 `StateFieldScanner.fromMap`，将 `Map<String, Object>` 反射注入到新创建的 POJO 实例中；注入范围包含完整继承链声明的字段。

#### Scenario: 正常字段注入
- **WHEN** 调用 `StateFieldScanner.fromMap(map, ChatState.class)` 且 Map 中有对应 key
- **THEN** 返回的 POJO 中对应字段被设置为 Map 中的值

#### Scenario: 父类字段也被注入
- **WHEN** Map 中包含父类声明字段对应的 key
- **THEN** 返回 POJO 实例中父类字段被正确设置，即使该字段为 `private`（通过 `setAccessible(true)` 访问）

#### Scenario: Map 中缺失 key 时字段保持默认值
- **WHEN** Map 中不存在某字段对应的 key
- **THEN** 返回的 POJO 中该字段保持无参构造器初始化后的默认值

---

### Requirement: StateFieldScanner 提取 KeyStrategy 映射
系统 SHALL 提供 `StateFieldScanner.getKeyStrategies`，从 POJO Class 中提取字段 key 到 `KeyStrategy` 的映射，供框架内部 `OverAllState.updateState` 使用；提取范围包含完整继承链。

#### Scenario: 标注字段使用声明的策略
- **WHEN** 调用 `StateFieldScanner.getKeyStrategies(ChatState.class)`
- **THEN** 返回 Map 中 `@StateField(strategy = AppendStrategy.class)` 的字段 key 对应 `AppendStrategy` 实例

#### Scenario: 未标注字段使用 ReplaceStrategy
- **WHEN** 字段未标注 `@StateField`
- **THEN** 返回 Map 中该字段 key 对应 `ReplaceStrategy` 实例

#### Scenario: 父类中标注的策略被继承
- **WHEN** 父类字段标注了 `@StateField(strategy = AppendStrategy.class)`，子类未覆盖同名字段
- **THEN** `getKeyStrategies` 结果中该 key 对应 `AppendStrategy` 实例

---

### Requirement: StateFieldScanner diff 计算
系统 SHALL 提供 `StateFieldScanner.diff`，比较两个 POJO 实例并返回变化的 delta Map，用于节点执行后的状态合并；diff 范围包含完整继承链。

#### Scenario: ReplaceStrategy 字段值变化时加入 delta
- **WHEN** 节点执行后 POJO 中某 ReplaceStrategy 字段值与快照不同
- **THEN** diff 结果 Map 中包含该字段 key 和新值

#### Scenario: ReplaceStrategy 字段值未变化时不加入 delta
- **WHEN** 节点执行后 POJO 中某 ReplaceStrategy 字段值与快照相同
- **THEN** diff 结果 Map 中不包含该字段 key

#### Scenario: AppendStrategy 字段提取末尾新增元素
- **WHEN** 节点执行后 AppendStrategy 字段的 List 长度大于快照 List 长度
- **THEN** diff 结果 Map 中包含该字段 key，值为 `newList.subList(snapshot.size(), newList.size())`

#### Scenario: AppendStrategy 字段无新增时不加入 delta
- **WHEN** 节点执行后 AppendStrategy 字段的 List 长度等于快照 List 长度
- **THEN** diff 结果 Map 中不包含该字段 key

#### Scenario: 父类字段的变化也被 diff 捕获
- **WHEN** 节点执行后父类声明的 ReplaceStrategy 字段值与快照不同
- **THEN** diff 结果 Map 中包含该父类字段的 key 和新值

---

### Requirement: StateFieldScanner 获取流式字段 key
系统 SHALL 提供 `StateFieldScanner.getStreamingFieldKey`，返回 State 类中标注 `@StateField(streaming = true)` 的字段对应的 Map key；搜索范围包含完整继承链，子类优先。

#### Scenario: 存在流式字段时返回其 key
- **WHEN** 调用 `StateFieldScanner.getStreamingFieldKey(ChatState.class)` 且存在 streaming=true 字段
- **THEN** 返回该字段的 key（`fieldName` 或 Java 字段名）

#### Scenario: 流式字段声明在父类中
- **WHEN** `streaming=true` 的字段声明在父类，子类未覆盖
- **THEN** 返回父类中该字段的 key

#### Scenario: 不存在流式字段时返回 null
- **WHEN** State 类中没有任何字段标注 `@StateField(streaming = true)`
- **THEN** 返回 null
