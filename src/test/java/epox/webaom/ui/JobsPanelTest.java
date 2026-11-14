/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.FlatLafSupport;
import epox.swing.FlatLafTheme;
import epox.swing.layout.TableColumnSizing;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.data.AniDBFile;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobsPanelTest {
    private String originalLookAndFeelClassName;

    @BeforeEach
    void setUp() {
        originalLookAndFeelClassName = UIManager.getLookAndFeel() == null
                ? null
                : UIManager.getLookAndFeel().getClass().getName();
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (originalLookAndFeelClassName != null) {
            UIManager.setLookAndFeel(originalLookAndFeelClassName);
        }
    }

    @Test
    void saveAndLoadOptions_preservesVisibleColumnsOrderWidthsAndHiddenState() {
        JobsPanel sourcePanel = createJobsPanel();
        sourcePanel.loadOptions(new Options());

        sourcePanel
                .getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));
        sourcePanel.getJobsTable().getColumnModel().getColumn(0).setPreferredWidth(321);
        sourcePanel.getJobsTable().getColumnModel().getColumn(1).setPreferredWidth(222);

        Options options = new Options();
        sourcePanel.saveOptions(options);

        JobsPanel restoredPanel = createJobsPanel();
        restoredPanel.loadOptions(options);

        assertEquals(2, restoredPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        assertEquals(
                321,
                restoredPanel
                        .getJobsTable()
                        .getHeaderListener()
                        .getColumnByModelIndex(JobColumn.STAT.getIndex())
                        .getPreferredWidth());
        assertEquals(
                222,
                restoredPanel
                        .getJobsTable()
                        .getHeaderListener()
                        .getColumnByModelIndex(JobColumn.NUMB.getIndex())
                        .getPreferredWidth());
        assertFalse(restoredPanel.getJobsTable().getHeaderListener().isColumnVisible(JobColumn.FILE.getIndex()));
    }

    @Test
    void loadOptions_legacyFormatRestoresVisibleColumnsAndHidesTheRest() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "14,200;0,60");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);

        assertEquals(2, panel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        assertFalse(panel.getJobsTable().getHeaderListener().isColumnVisible(JobColumn.FILE.getIndex()));
    }

    @Test
    void loadOptions_withoutSavedColumnConfig_appliesScaleFriendlyDefaults() {
        JobsPanel panel = createJobsPanel();
        panel.loadOptions(new Options());

        assertDefaultColumnsShown(panel);

        int titleWidth = panel.getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.TITL.getIndex())
                .getPreferredWidth();
        int statusWidth = panel.getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.STAT.getIndex())
                .getPreferredWidth();

        assertTrue(statusWidth >= 140);
        assertTrue(titleWidth >= 360);
        assertTrue(titleWidth > statusWidth);
    }

    @Test
    void loadOptions_keepsASavedLayoutEvenWhenItMatchesTheOldDefaults() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "0,48;11,360;14,140;");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);

        assertVisibleColumns(panel, JobColumn.NUMB, JobColumn.FILE, JobColumn.STAT);
    }

    @Test
    void resetColumnsInTheHeaderMenu_restoresDefaultColumnsAndWidths() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "11,500;0,80;");
        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);
        JTableJobs table = panel.getJobsTable();
        table.getHeaderListener()
                .getColumnByModelIndex(JobColumn.TITL.getIndex())
                .setPreferredWidth(20);

        headerMenuItem(table, "Reset Columns").doClick();

        assertDefaultColumnsShown(panel);
        assertTrue(table.getHeaderListener()
                        .getColumnByModelIndex(JobColumn.TITL.getIndex())
                        .getPreferredWidth()
                >= 360);
        JCheckBoxMenuItem titleItem = (JCheckBoxMenuItem) headerMenuItem(table, "Title");
        assertTrue(titleItem.isSelected());
    }

    @Test
    void loadOptions_customLayoutWithFilePath_isKept() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "11,500;14,140;");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);

        assertVisibleColumns(panel, JobColumn.FILE, JobColumn.STAT);
    }

    @Test
    void rowNumberColumn_cannotShrinkBelowItsContent() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "0,8;14,140;");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);

        JTableJobs table = panel.getJobsTable();
        TableColumn rowColumn = table.getHeaderListener().getColumnByModelIndex(JobColumn.NUMB.getIndex());
        int contentWidth = TableColumnSizing.calculatePreferredWidth(
                table, rowColumn.getHeaderValue(), 0, TableModelJobs.ROW_NUMBER_SAMPLE);
        assertTrue(rowColumn.getMinWidth() >= contentWidth);
        assertTrue(rowColumn.getPreferredWidth() >= contentWidth);
    }

    @Test
    void scaleCurrentColumnWidths_scalesTheRowNumberColumnOnce() {
        Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "0,8;14,140;");
        JobsPanel panel = createJobsPanel(smallFont);
        panel.loadOptions(options);
        JTableJobs table = panel.getJobsTable();
        TableColumn rowColumn = table.getHeaderListener().getColumnByModelIndex(JobColumn.NUMB.getIndex());
        int widthBefore = rowColumn.getPreferredWidth();

        Font largeFont = smallFont.deriveFont(24f);
        table.setFont(largeFont);
        table.getTableHeader().setFont(largeFont);
        panel.scaleCurrentColumnWidths(2.0);

        int largeContentWidth = TableColumnSizing.calculatePreferredWidth(
                table, rowColumn.getHeaderValue(), 0, TableModelJobs.ROW_NUMBER_SAMPLE);
        assertEquals(Math.max(widthBefore * 2, largeContentWidth), rowColumn.getPreferredWidth());
    }

    @Test
    void reapplyScaleAwareSizing_keepsColumnsShownAfterReset() {
        JobsPanel panel = createJobsPanel();
        panel.loadOptions(new Options());
        JTableJobs table = panel.getJobsTable();

        headerMenuItem(table, JobColumn.FILE.getDescription()).doClick();
        panel.reapplyScaleAwareSizing();

        assertVisibleColumns(panel, JobColumn.FILE, JobColumn.TITL, JobColumn.STAT);
    }

    @Test
    void statusColumnHiddenDuringThemeChange_drawsProgressInTheNewTheme() throws Exception {
        Path directory = Files.createTempDirectory("jobs-panel-progress-theme-test");
        Job hashingJob = createExistingJob(directory, "hashing.mkv", Job.HASHING);
        AppContext.jobs.add(hashingJob);
        UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        JobsPanel panel = createJobsPanel();
        JTableJobs table = panel.getJobsTable();
        table.getHeaderListener().setVisibleColumns(List.of(JobColumn.FILE.getIndex()));

        FlatLafSupport.applyTheme(FlatLafTheme.DARK);
        SwingUtilities.updateComponentTreeUI(panel);
        table.getHeaderListener().setVisibleColumns(List.of(JobColumn.STAT.getIndex()));

        JProgressBar progressBar = assertInstanceOf(JProgressBar.class, renderedComponent(table, 0, JobColumn.STAT));
        assertEquals(new JProgressBar().getUI().getClass(), progressBar.getUI().getClass());
    }

    @Test
    void pathColumns_measureTheCellNotTheColumnMargin() throws Exception {
        Path directory = Files.createTempDirectory("jobs-panel-margin-test");
        Job job = createExistingJob(directory, "Show - 05.mkv", Job.FINISHED);
        AppContext.jobs.add(job);
        JTableJobs table = createJobsTable();
        table.setIntercellSpacing(new java.awt.Dimension(20, 0));
        TableColumn fileColumn = table.getHeaderListener().getColumnByModelIndex(JobColumn.FILE.getIndex());
        JLabel label = (JLabel) renderedComponent(table, 0, JobColumn.FILE);
        java.awt.Insets insets = label.getInsets();
        int textWidth =
                label.getFontMetrics(label.getFont()).stringWidth(job.getFile().getAbsolutePath());

        // Wide enough for the text only if the margin between columns were part of the cell
        fileColumn.setWidth(textWidth + insets.left + insets.right);

        assertTrue(renderedText(table, 0, JobColumn.FILE).startsWith("…"));
    }

    @Test
    void defaultColumnConfig_growsWithLargerFont() {
        JobsPanel smallFontPanel = createJobsPanel(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        smallFontPanel.loadOptions(new Options());
        int smallTitleWidth = smallFontPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.TITL.getIndex())
                .getPreferredWidth();

        JobsPanel largeFontPanel = createJobsPanel(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        largeFontPanel.loadOptions(new Options());
        int largeTitleWidth = largeFontPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.TITL.getIndex())
                .getPreferredWidth();

        assertTrue(largeTitleWidth > smallTitleWidth);
    }

    @Test
    void hoveringARow_showsTheJobsFullPath() throws Exception {
        Path directory = Files.createTempDirectory("jobs-panel-tooltip-test");
        Job job = createExistingJob(directory, "episode.mkv", Job.FINISHED);
        AppContext.jobs.add(job);
        JTableJobs table = createJobsTable();

        java.awt.Rectangle cell = table.getCellRect(0, 0, true);
        MouseEvent hover = new MouseEvent(table, MouseEvent.MOUSE_MOVED, 0, 0, cell.x + 2, cell.y + 2, 0, false);

        assertEquals(job.getFile().getAbsolutePath(), table.getToolTipText(hover));
    }

    @Test
    void pathColumns_truncateFromTheStartSoTheFilenameStaysVisible() throws Exception {
        Path directory = Files.createDirectories(Files.createTempDirectory("jobs-panel-path-test")
                .resolve("a very long library folder")
                .resolve("another deeply nested folder"));
        Job job = createExistingJob(directory, "Show - 05.mkv", Job.FINISHED);
        AppContext.jobs.add(job);
        JTableJobs table = createJobsTable();
        TableColumn fileColumn = table.getHeaderListener().getColumnByModelIndex(JobColumn.FILE.getIndex());

        fileColumn.setWidth(160);
        String narrowText = renderedText(table, 0, JobColumn.FILE);
        assertTrue(narrowText.startsWith("\u2026"), narrowText);
        assertTrue(narrowText.endsWith("Show - 05.mkv"), narrowText);

        fileColumn.setWidth(4000);
        assertEquals(job.getFile().getAbsolutePath(), renderedText(table, 0, JobColumn.FILE));
    }

    @Test
    void tableHeader_hintsThatRightClickChoosesColumns() {
        JTableJobs table = createJobsTable();

        assertTrue(table.getTableHeader().getToolTipText().contains("right-click"));
    }

    @Test
    void statusColumn_drawsHashingJobsAsAProgressBar() throws Exception {
        Path directory = Files.createTempDirectory("jobs-panel-progress-test");
        Job hashingJob = createExistingJob(directory, "hashing.mkv", Job.HASHING);
        hashingJob.hashProgress = 0.34f;
        Job finishedJob = createExistingJob(directory, "finished.mkv", Job.FINISHED);
        AppContext.jobs.add(hashingJob);
        AppContext.jobs.add(finishedJob);
        JTableJobs table = createJobsTable();

        java.awt.Component hashingCell = renderedComponent(table, 0, JobColumn.STAT);
        JProgressBar progressBar = assertInstanceOf(JProgressBar.class, hashingCell);
        assertEquals(34, progressBar.getValue());
        assertTrue(progressBar.isStringPainted());
        assertEquals("Hashing 34%", progressBar.getString());

        assertEquals("Finished", renderedText(table, 1, JobColumn.STAT));
    }

    @Test
    void setVisibleColumns_keepsAtLeastOneColumnVisible() {
        JobsPanel panel = createJobsPanel();

        panel.getJobsTable().getHeaderListener().setVisibleColumns(java.util.Collections.emptyList());

        assertEquals(1, panel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
    }

    @Test
    void formatTable_alignsColumnsByDataKind() {
        JTableJobs jobsTable = createJobsTable();

        assertEquals(SwingConstants.RIGHT, getAlignment(jobsTable, JobColumn.NUMB));
        assertEquals(SwingConstants.CENTER, getAlignment(jobsTable, JobColumn.STAT));
        assertEquals(SwingConstants.LEFT, getAlignment(jobsTable, JobColumn.FILE));
    }

    @Test
    void updateComponentTreeUi_preservesHiddenColumnsForLaterSave() {
        JobsPanel panel = createJobsPanel();
        panel.getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));

        SwingUtilities.updateComponentTreeUI(panel);

        assertEquals(2, panel.getJobsTable().getColumnModel().getColumnCount());

        Options options = new Options();
        panel.saveOptions(options);

        JobsPanel restoredPanel = createJobsPanel();
        restoredPanel.loadOptions(options);

        assertEquals(2, restoredPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
    }

    @Test
    void saveOptions_usesLegacyVisibleOnlyFormatForCrossVersionCompatibility() {
        JobsPanel panel = createJobsPanel();
        panel.getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));
        panel.getJobsTable().getColumnModel().getColumn(0).setPreferredWidth(200);
        panel.getJobsTable().getColumnModel().getColumn(1).setPreferredWidth(60);

        Options options = new Options();
        panel.saveOptions(options);

        assertEquals("14,200;0,60;", options.getString(Options.STR_JOB_COLUMNS));
    }

    @Test
    void loadOptions_newFormatThenSave_rewritesToLegacyVisibleOnlyFormat() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "14,200,1;0,60,1;11,973,0;");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);
        panel.saveOptions(options);

        assertEquals("14,200;0,60;", options.getString(Options.STR_JOB_COLUMNS));
    }

    @Test
    void prepareRenderer_usesThemeAwareReadableColorsInDarkAndLightThemes() throws Exception {
        verifyRendererColors(FlatLafTheme.DARK, true);
        verifyRendererColors(FlatLafTheme.LIGHT, false);
    }

    private static JMenuItem headerMenuItem(JTableJobs table, String label) {
        for (java.awt.Component component :
                table.getHeaderListener().getColumnPopup().getComponents()) {
            if (component instanceof JMenuItem item && label.equals(item.getText())) {
                return item;
            }
        }
        throw new AssertionError("No header menu item labelled " + label);
    }

    private static void assertDefaultColumnsShown(JobsPanel panel) {
        assertVisibleColumns(panel, JobColumn.TITL, JobColumn.STAT);
    }

    static void assertVisibleColumns(JobsPanel panel, JobColumn... expected) {
        TableColumnModel columnModel = panel.getJobsTable().getColumnModel();
        List<JobColumn> shown = new ArrayList<>();
        for (int index = 0; index < columnModel.getColumnCount(); index++) {
            shown.add(JobColumn.fromIndex(columnModel.getColumn(index).getModelIndex()));
        }
        assertEquals(List.of(expected), shown);
    }

    private java.awt.Component renderedComponent(JTableJobs table, int row, JobColumn column) {
        int columnIndex = table.convertColumnIndexToView(column.getIndex());
        return table.prepareRenderer(table.getCellRenderer(row, columnIndex), row, columnIndex);
    }

    private String renderedText(JTableJobs table, int row, JobColumn column) {
        return ((JLabel) renderedComponent(table, row, column)).getText();
    }

    private JobsPanel createJobsPanel() {
        return createJobsPanel(null);
    }

    private JobsPanel createJobsPanel(Font font) {
        TableModelJobs tableModel = new TableModelJobs(AppContext.jobs);
        JTableJobs jobsTable = new JTableJobs(tableModel);
        if (font != null) {
            jobsTable.setFont(font);
            if (jobsTable.getTableHeader() != null) {
                jobsTable.getTableHeader().setFont(font);
            }
        }
        TableModelJobs.formatTable(jobsTable);
        return new JobsPanel(jobsTable, tableModel);
    }

    private JTableJobs createJobsTable() {
        return createJobsPanel().getJobsTable();
    }

    private int getAlignment(JTableJobs jobsTable, JobColumn column) {
        TableColumn tableColumn = jobsTable.getColumnModel().getColumn(column.getIndex());
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        return ((DefaultTableCellRenderer) renderer).getHorizontalAlignment();
    }

    private void verifyRendererColors(FlatLafTheme theme, boolean darkTheme) throws Exception {
        FlatLafSupport.applyTheme(theme);

        Path tempDirectory = Files.createTempDirectory("jobs-panel-theme-test");

        Job normalJob = createExistingJob(tempDirectory, "normal.mkv", Job.FINISHED);
        Job diskIoJob = createExistingJob(tempDirectory, "hashing.mkv", Job.HASHING);
        Job networkIoJob = createExistingJob(tempDirectory, "identifying.mkv", Job.IDENTIFYING);
        Job missingJob = createMissingJob(tempDirectory.resolve("missing.mkv").toFile(), Job.FINISHED);
        Job corruptJob = createCorruptJob(tempDirectory, "corrupt.mkv");
        Job selectedJob = createExistingJob(tempDirectory, "selected.mkv", Job.FINISHED);

        AppContext.jobs.add(normalJob);
        AppContext.jobs.add(diskIoJob);
        AppContext.jobs.add(networkIoJob);
        AppContext.jobs.add(missingJob);
        AppContext.jobs.add(corruptJob);
        AppContext.jobs.add(selectedJob);

        JTableJobs jobsTable = createJobsTable();
        jobsTable.setRowSelectionInterval(5, 5);

        Color tableForeground = jobsTable.getForeground();
        Color tableBackground = jobsTable.getBackground();

        RenderedCell normalFileCell = renderCell(jobsTable, 0, JobColumn.FILE);
        RenderedCell normalStatusCell = renderCell(jobsTable, 0, JobColumn.STAT);
        RenderedCell diskFileCell = renderCell(jobsTable, 1, JobColumn.FILE);
        RenderedCell networkStatusCell = renderCell(jobsTable, 2, JobColumn.STAT);
        RenderedCell missingStatusCell = renderCell(jobsTable, 3, JobColumn.STAT);
        RenderedCell corruptStatusCell = renderCell(jobsTable, 4, JobColumn.STAT);
        RenderedCell selectedStatusCell = renderCell(jobsTable, 5, JobColumn.STAT);

        assertEquals(tableForeground, normalFileCell.foreground());
        assertEquals(tableForeground, normalStatusCell.foreground());
        assertEquals(tableBackground, normalFileCell.background());
        assertEquals(tableBackground, normalStatusCell.background());

        if (darkTheme) {
            assertNotEquals(Color.black, normalFileCell.foreground());
            assertNotEquals(Color.black, normalStatusCell.foreground());
        }

        assertTrue(contrastRatio(diskFileCell.foreground(), diskFileCell.background()) >= 3.0);
        assertTrue(contrastRatio(networkStatusCell.foreground(), networkStatusCell.background()) >= 3.0);
        assertTrue(contrastRatio(missingStatusCell.foreground(), missingStatusCell.background()) >= 3.0);
        assertTrue(contrastRatio(corruptStatusCell.foreground(), corruptStatusCell.background()) >= 4.5);

        assertNotEquals(tableForeground, diskFileCell.foreground());
        assertNotEquals(tableForeground, networkStatusCell.foreground());
        assertNotEquals(tableForeground, missingStatusCell.foreground());
        assertNotEquals(tableBackground, corruptStatusCell.background());

        assertEquals(jobsTable.getSelectionForeground(), selectedStatusCell.foreground());
        assertEquals(jobsTable.getSelectionBackground(), selectedStatusCell.background());
    }

    private Job createExistingJob(Path directory, String fileName, int status) throws IOException {
        File file = Files.createFile(directory.resolve(fileName)).toFile();
        file.deleteOnExit();
        Job job = new Job(file, status);
        job.setStatus(status, false);
        return job;
    }

    private Job createMissingJob(File file, int status) {
        return new Job(file, status);
    }

    private Job createCorruptJob(Path directory, String fileName) throws IOException {
        Job job = createExistingJob(directory, fileName, Job.FINISHED);
        AniDBFile anidbFile = new AniDBFile(123);
        anidbFile.setState(AniDBFile.F_CRCERR);
        job.anidbFile = anidbFile;
        return job;
    }

    private RenderedCell renderCell(JTableJobs jobsTable, int row, JobColumn column) {
        int columnIndex = jobsTable.convertColumnIndexToView(column.getIndex());
        java.awt.Component component =
                jobsTable.prepareRenderer(jobsTable.getCellRenderer(row, columnIndex), row, columnIndex);
        return new RenderedCell(component.getForeground(), component.getBackground());
    }

    private double contrastRatio(Color foreground, Color background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(Color color) {
        double red = linearize(color.getRed() / 255.0);
        double green = linearize(color.getGreen() / 255.0);
        double blue = linearize(color.getBlue() / 255.0);
        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    private double linearize(double channel) {
        if (channel <= 0.03928) {
            return channel / 12.92;
        }
        return Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private record RenderedCell(Color foreground, Color background) {}
}
