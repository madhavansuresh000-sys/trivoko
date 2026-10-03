param([string]$Docx, [string]$Pdf)
# Opens the generated .docx in Word, fills in the table of contents and page numbers,
# saves it again and exports a PDF. Prints the page count.
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Open($Docx)
foreach ($toc in $doc.TablesOfContents) { $toc.Update() }
$doc.Fields.Update() | Out-Null
$doc.Repaginate()
$pages = $doc.ComputeStatistics(2)
$words = $doc.ComputeStatistics(0)
$doc.Save()
if ($Pdf) { $doc.SaveAs2($Pdf, 17) }
$doc.Close()
$word.Quit()
"pages=$pages words=$words"
