package jurate.printers

import jurate.*

/** An error printer that formats configuration errors as ASCII tables.
  *
  * This printer creates well-formatted tables with columns for field names, sources
  * (environment variables or system properties), and error messages. It includes
  * word wrapping and column width management for readable output.
  */
object TablePrinter extends ErrorPrinter {

  private case class TableRow(field: String, source: String, message: String)
  private case class ColumnWidths(field: Int, source: Int, message: Int)

  /** Formats a configuration error as an ASCII table.
    *
    * @param error the configuration error to format
    * @return a formatted table string showing all error reasons
    */
  override def format(error: ConfigError): String = {
    val rows = error.reasons.map(reasonToRow)
    renderTable(rows)
  }

  private def reasonToRow(reason: ConfigErrorReason): TableRow = reason match {
    case Missing(fieldPath, annotations) =>
      TableRow(
        field = fieldPath.dottedPath,
        source = formatAnnotations(annotations),
        message = "Missing configuration value"
      )

    case Invalid(receivedValue, detail, fieldPath, annotationOpt) =>
      TableRow(
        field = fieldPath.dottedPath,
        source =
          annotationOpt.map(ann => formatAnnotations(Seq(ann))).getOrElse(""),
        message = s"Invalid value: $detail, received: '$receivedValue'"
      )

    case Other(fieldPath, detail, annotationOpt) =>
      TableRow(
        field = fieldPath.dottedPath,
        source =
          annotationOpt.map(ann => formatAnnotations(Seq(ann))).getOrElse(""),
        message = detail
      )
  }

  private def formatAnnotations(annotations: Seq[ConfigAnnotation]): String =
    annotations
      .map {
        case env(name) => s"$name (env)"
        case prop(path) => s"$path (prop)"
      }
      .mkString(", ")

  private def wordWrap(text: String, maxWidth: Int): List[String] =
    if (text.length <= maxWidth) List(text)
    else {
      text.split(" ").foldLeft(List.empty[String] -> "") {
        case ((lines, currentLine), word) =>
          if (currentLine.isEmpty)
            (lines, word)
          else if ((currentLine + " " + word).length <= maxWidth)
            (lines, currentLine + " " + word)
          else
            (lines :+ currentLine, word)
      } match {
        case (lines, lastLine) if lastLine.nonEmpty => lines :+ lastLine
        case (lines, _) => lines
      }
    }

  private def calculateColumnWidths(rows: List[TableRow]): ColumnWidths = {
    val FieldHeader = "Field"
    val SourceHeader = "Source"
    val MessageHeader = "Message"

    val maxFieldWidth = 60
    val maxSourceWidth = 80
    val maxMessageWidth = 150

    ColumnWidths(
      field = math.min(
        maxFieldWidth,
        math.max(
          FieldHeader.length,
          rows.map(_.field.length).maxOption.getOrElse(0)
        )
      ),
      source = math.min(
        maxSourceWidth,
        math.max(
          SourceHeader.length,
          rows.map(_.source.length).maxOption.getOrElse(0)
        )
      ),
      message = math.min(
        maxMessageWidth,
        math.max(
          MessageHeader.length,
          rows.map(_.message.length).maxOption.getOrElse(0)
        )
      )
    )
  }

  private def createTopBorder(widths: ColumnWidths): String =
    s"┌─${"─" * widths.field}─┬─${"─" * widths.source}─┬─${"─" * widths.message}─┐"

  private def createSeparator(widths: ColumnWidths): String =
    s"├─${"─" * widths.field}─┼─${"─" * widths.source}─┼─${"─" * widths.message}─┤"

  private def createBottomBorder(widths: ColumnWidths): String =
    s"└─${"─" * widths.field}─┴─${"─" * widths.source}─┴─${"─" * widths.message}─┘"

  private def createHeaderRow(widths: ColumnWidths): String = {
    val headers = List("Field", "Source", "Message")
    s"│ ${headers(0).padTo(widths.field, ' ')} │ ${headers(1).padTo(widths.source, ' ')} │ ${headers(2).padTo(widths.message, ' ')} │"
  }

  private def createDataRow(
      field: String,
      source: String,
      message: String,
      widths: ColumnWidths
  ): String =
    s"│ ${field.take(widths.field).padTo(widths.field, ' ')} │ ${source.take(widths.source).padTo(widths.source, ' ')} │ ${message.padTo(widths.message, ' ')} │"

  private def createWrappedMessageRow(
      message: String,
      widths: ColumnWidths
  ): String =
    s"│ ${" " * widths.field} │ ${" " * widths.source} │ ${message.padTo(widths.message, ' ')} │"

  private def renderRow(
      row: TableRow,
      widths: ColumnWidths,
      isLast: Boolean
  ): List[String] = {
    val messageLines = wordWrap(row.message, widths.message)
    val firstLine = createDataRow(
      row.field,
      row.source,
      messageLines.headOption.getOrElse(""),
      widths
    )
    val wrappedLines =
      messageLines.drop(1).map(line => createWrappedMessageRow(line, widths))
    val separator = if (isLast) Nil else List(createSeparator(widths))

    firstLine :: wrappedLines ::: separator
  }

  private def renderTable(rows: List[TableRow]): String =
    if (rows.isEmpty) "No errors"
    else {
      val widths = calculateColumnWidths(rows)
      val header = List(
        createTopBorder(widths),
        createHeaderRow(widths),
        createSeparator(widths)
      )
      val dataRows = rows.zipWithIndex.flatMap { case (row, idx) =>
        renderRow(row, widths, isLast = idx == rows.length - 1)
      }
      val footer = List(createBottomBorder(widths))

      (header ::: dataRows ::: footer).mkString("\n")
    }
}
