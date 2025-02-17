// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.dsl.impl

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.CellBuilder
import com.intellij.ui.dsl.RightGap
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.*
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus
import javax.swing.JComponent

@ApiStatus.Experimental
internal class CellBuilderImpl<T : JComponent>(
  private val dialogPanelConfig: DialogPanelConfig,
  component: T,
  val viewComponent: JComponent = component) : CellBuilderBaseImpl<CellBuilder<T>>(dialogPanelConfig), CellBuilder<T> {

  override var component: T = component
    private set

  private var property: GraphProperty<*>? = null
  private var applyIfEnabled = false

  override fun horizontalAlign(horizontalAlign: HorizontalAlign): CellBuilder<T> {
    super.horizontalAlign(horizontalAlign)
    return this
  }

  override fun verticalAlign(verticalAlign: VerticalAlign): CellBuilder<T> {
    super.verticalAlign(verticalAlign)
    return this
  }

  override fun resizableColumn(): CellBuilder<T> {
    super.resizableColumn()
    return this
  }

  override fun comment(@NlsContexts.DetailedDescription comment: String, maxLineLength: Int): CellBuilder<T> {
    super.comment(comment, maxLineLength)
    return this
  }

  override fun gap(rightGap: RightGap): CellBuilder<T> {
    super.gap(rightGap)
    return this
  }

  override fun applyToComponent(task: T.() -> Unit): CellBuilder<T> {
    component.task()
    return this
  }

  override fun enabled(isEnabled: Boolean): CellBuilder<T> {
    viewComponent.isEnabled = isEnabled
    return this
  }

  override fun visibleIf(predicate: ComponentPredicate): CellBuilder<T> {
    viewComponent.isVisible = predicate()
    predicate.addListener { viewComponent.isVisible = it }
    return this
  }

  override fun applyIfEnabled(): CellBuilder<T> {
    applyIfEnabled = true
    return this
  }

  override fun <V> bind(componentGet: (T) -> V, componentSet: (T, V) -> Unit, binding: PropertyBinding<V>): CellBuilder<T> {
    onApply { if (shouldSaveOnApply()) binding.set(componentGet(component)) }
    onReset { componentSet(component, binding.get()) }
    onIsModified { shouldSaveOnApply() && componentGet(component) != binding.get() }
    return this
  }

  private fun shouldSaveOnApply(): Boolean {
    return !(applyIfEnabled && !viewComponent.isEnabled)
  }

  fun onValidationOnInput(callback: ValidationInfoBuilder.(T) -> ValidationInfo?): CellBuilderImpl<T> {
    val origin = component.origin
    dialogPanelConfig.componentValidateCallbacks[origin] = { callback(ValidationInfoBuilder(origin), component) }
    property?.let { dialogPanelConfig.customValidationRequestors.getOrPut(origin, { SmartList() }).add(it::afterPropagation) }
    return this
  }

  private fun onApply(callback: () -> Unit): CellBuilder<T> {
    dialogPanelConfig.applyCallbacks.register(component, callback)
    return this
  }

  private fun onReset(callback: () -> Unit): CellBuilder<T> {
    dialogPanelConfig.resetCallbacks.register(component, callback)
    return this
  }

  private fun onIsModified(callback: () -> Boolean): CellBuilder<T> {
    dialogPanelConfig.isModifiedCallbacks.register(component, callback)
    return this
  }
}

private val JComponent.origin: JComponent
  get() {
    return when (this) {
      is TextFieldWithBrowseButton -> textField
      else -> this
    }
  }