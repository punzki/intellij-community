// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.dsl.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.TitledSeparator
import com.intellij.ui.dsl.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.JBGrid
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.dsl.gridLayout.builders.RowsGridBuilder
import org.jetbrains.annotations.ApiStatus
import javax.swing.*
import javax.swing.text.JTextComponent
import kotlin.math.min

@ApiStatus.Experimental
internal class PanelBuilderImpl(private val dialogPanelConfig: DialogPanelConfig) : CellBuilderBaseImpl<PanelBuilder>(
  dialogPanelConfig), PanelBuilder {

  companion object {
    private val LOG = Logger.getInstance(PanelBuilderImpl::class.java)

    private val DEFAULT_VERTICAL_GAP_COMPONENTS = setOf(
      AbstractButton::class,
      JComboBox::class,
      JLabel::class,
      JSpinner::class,
      JTextComponent::class,
      TitledSeparator::class
    )
  }

  private val rows = mutableListOf<RowBuilderImpl>()

  override fun row(label: JLabel?, init: RowBuilder.() -> Unit): RowBuilder {
    val result = RowBuilderImpl(dialogPanelConfig, label)
    result.init()
    rows.add(result)
    return result
  }

  override fun group(title: String?, independent: Boolean, init: PanelBuilder.() -> Unit): RowBuilder {
    val component = createSeparator(title)
    if (independent) {
      return row {
        val panel = panel {
          row {
            cell(component)
              .horizontalAlign(HorizontalAlign.FILL)
          }
        }

        panel.init()
      }.gap(TopGap.GROUP)
    }

    // todo
    return RowBuilderImpl(dialogPanelConfig)
  }

  override fun horizontalAlign(horizontalAlign: HorizontalAlign): PanelBuilder {
    super.horizontalAlign(horizontalAlign)
    return this
  }

  override fun verticalAlign(verticalAlign: VerticalAlign): PanelBuilder {
    super.verticalAlign(verticalAlign)
    return this
  }

  override fun resizableColumn(): PanelBuilder {
    super.resizableColumn()
    return this
  }

  override fun comment(@NlsContexts.DetailedDescription comment: String, maxLineLength: Int): PanelBuilder {
    super.comment(comment, maxLineLength)
    return this
  }

  override fun gap(rightGap: RightGap): PanelBuilder {
    super.gap(rightGap)
    return this
  }

  fun build(panel: DialogPanel, grid: JBGrid) {
    val maxColumnsCount = getMaxColumnsCount()
    val rowsGridBuilder = RowsGridBuilder(panel, grid = grid)

    for ((index, row) in rows.withIndex()) {
      if (row.cells.isEmpty()) {
        LOG.warn("Row should not be empty")
        continue
      }

      row.topGap?.let {
        if (index > 0) {
          rowsGridBuilder.addBeforeDistance(rowGapDistance(it))
        }
      }

      when (row.rowLayout) {
        RowLayout.INDEPENDENT -> {
          val subGrid = rowsGridBuilder.subGrid(width = maxColumnsCount, horizontalAlign = HorizontalAlign.FILL,
                                                verticalAlign = VerticalAlign.FILL)
          val subGridBuilder = RowsGridBuilder(panel, subGrid)
          val cells = row.cells
          buildRow(cells, row.label != null, cells.size, panel, subGridBuilder)
          subGridBuilder.row()

          buildCommentRow(cells, cells.size, subGridBuilder)
          setLastColumnResizable(subGridBuilder)
          rowsGridBuilder.row()
        }

        RowLayout.LABEL_ALIGNED -> {
          buildCell(row.cells[0], true, row.cells.size == 1, 1, panel, rowsGridBuilder)

          if (row.cells.size > 1) {
            val subGrid = rowsGridBuilder.subGrid(width = maxColumnsCount - 1, horizontalAlign = HorizontalAlign.FILL,
                                                  verticalAlign = VerticalAlign.FILL)
            val subGridBuilder = RowsGridBuilder(panel, subGrid)
            val cells = row.cells.subList(1, row.cells.size)
            buildRow(cells, false, cells.size, panel, subGridBuilder)
            setLastColumnResizable(subGridBuilder)
          }
          rowsGridBuilder.row()

          val commentedCellIndex = getCommentedCellIndex(row.cells)
          when {
            commentedCellIndex in 0..1 -> {
              buildCommentRow(row.cells, maxColumnsCount, rowsGridBuilder)
            }
            commentedCellIndex > 1 -> {
              // Always put comment for cells with index more than 1 at second cell because it's hard to implement
              // more correct behaviour now. Can be fixed later
              buildCommentRow(listOf(row.cells[0], row.cells[commentedCellIndex]), maxColumnsCount, rowsGridBuilder)
            }
          }
        }

        RowLayout.PARENT_GRID -> {
          buildRow(row.cells, row.label != null, maxColumnsCount, panel, rowsGridBuilder)
          rowsGridBuilder.row()

          buildCommentRow(row.cells, maxColumnsCount, rowsGridBuilder)
        }
      }

      row.comment?.let {
        val gaps = Gaps(bottom = dialogPanelConfig.spacing.verticalCommentBottomGap)
        rowsGridBuilder.cell(it, maxColumnsCount, gaps = gaps)
        rowsGridBuilder.row()
      }
    }

    setLastColumnResizable(rowsGridBuilder)
  }

  private fun setLastColumnResizable(builder: RowsGridBuilder) {
    if (builder.resizableColumns.isEmpty() && builder.columnsCount > 0) {
      builder.resizableColumns = setOf(builder.columnsCount - 1)
    }
  }

  private fun buildRow(cells: List<CellBuilderBaseImpl<*>>,
                       firstCellLabel: Boolean,
                       maxColumnsCount: Int,
                       panel: DialogPanel,
                       builder: RowsGridBuilder) {
    for ((cellIndex, cell) in cells.withIndex()) {
      val lastCell = cellIndex == cells.size - 1
      val width = if (lastCell) maxColumnsCount - cellIndex else 1
      buildCell(cell, firstCellLabel && cellIndex == 0, lastCell, width, panel, builder)
    }
  }

  private fun buildCell(cell: CellBuilderBaseImpl<*>, rowLabel: Boolean, lastCell: Boolean, width: Int,
                        panel: DialogPanel, builder: RowsGridBuilder) {
    val rightGap = getRightGap(cell, lastCell, rowLabel)

    when (cell) {
      is CellBuilderImpl<*> -> {
        val insets = cell.component.insets
        val visualPaddings = Gaps(top = insets.top, left = insets.left, bottom = insets.bottom, right = insets.right)
        val verticalGap = getDefaultVerticalGap(cell.component)
        val gaps = Gaps(top = verticalGap, bottom = verticalGap, right = rightGap)
        builder.cell(cell.component, width = width, horizontalAlign = cell.horizontalAlign, verticalAlign = cell.verticalAlign,
                     resizableColumn = cell.resizableColumn,
                     gaps = gaps, visualPaddings = visualPaddings)
      }
      is PanelBuilderImpl -> {
        // todo visualPaddings
        val gaps = Gaps(right = rightGap)
        val subGrid = builder.subGrid(width = width, horizontalAlign = cell.horizontalAlign, verticalAlign = cell.verticalAlign,
                                      gaps = gaps)

        cell.build(panel, subGrid)
      }
    }
  }

  /**
   * Returns default top and bottom gap for [component]
   */
  private fun getDefaultVerticalGap(component: JComponent): Int {
    return if (DEFAULT_VERTICAL_GAP_COMPONENTS.any { clazz ->
        clazz.isInstance(component)
      }) dialogPanelConfig.spacing.verticalComponentGap
    else 0
  }

  private fun getMaxColumnsCount(): Int {
    return rows.maxOf {
      when (it.rowLayout) {
        RowLayout.INDEPENDENT -> 1
        RowLayout.LABEL_ALIGNED -> min(2, it.cells.size)
        RowLayout.PARENT_GRID -> it.cells.size
      }
    }
  }

  private fun getRightGap(cell: CellBuilderBaseImpl<*>, lastCell: Boolean, rowLabel: Boolean): Int {
    val rightGap = cell.rightGap
    if (lastCell) {
      if (rightGap != null) {
        LOG.warn("Right gap is set for last cell and will be ignored: rightGap = $rightGap")
      }
      return 0
    }

    if (rightGap != null) {
      return when(rightGap) {
        RightGap.SMALL -> dialogPanelConfig.spacing.horizontalSmallGap
      }
    }

    return if (rowLabel) dialogPanelConfig.spacing.horizontalSmallGap else dialogPanelConfig.spacing.horizontalDefaultGap
  }


  /**
   * Appends comment (currently one comment for a row is supported, can be fixed later)
   */
  private fun buildCommentRow(cells: List<CellBuilderBaseImpl<*>>,
                              maxColumnsCount: Int,
                              builder: RowsGridBuilder) {
    val commentedCellIndex = getCommentedCellIndex(cells)
    if (commentedCellIndex < 0) {
      return
    }

    val cell = cells[commentedCellIndex]
    val gaps = Gaps(left = getAdditionalHorizontalIndent(cell), bottom = dialogPanelConfig.spacing.verticalCommentBottomGap)
    builder.skip(commentedCellIndex)
    builder.cell(cell.comment!!, maxColumnsCount - commentedCellIndex, gaps = gaps)
    builder.row()
    return
  }

  private fun getAdditionalHorizontalIndent(cell: CellBuilderBaseImpl<*>): Int {
    return if (cell is CellBuilderImpl<*> && cell.viewComponent is JToggleButton)
      dialogPanelConfig.spacing.horizontalIndent
    else
      0
  }

  private fun getCommentedCellIndex(cells: List<CellBuilderBaseImpl<*>>): Int {
    return cells.indexOfFirst { it.comment != null }
  }

  private fun rowGapDistance(topGap: TopGap): Int {
    return when(topGap) {
      TopGap.GROUP -> dialogPanelConfig.spacing.verticalGroupTopGap
    }
  }

  private fun createSeparator(@NlsContexts.BorderTitle title: String?): JComponent {
    if (title == null) {
      return SeparatorComponent(0, OnePixelDivider.BACKGROUND, null)
    }

    val result = TitledSeparator(title)
    result.border = null
    return result
  }
}
