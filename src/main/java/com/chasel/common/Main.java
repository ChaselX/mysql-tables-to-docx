package com.chasel.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XieLongzhen
 * @date 2019/9/23 16:48
 */
public class Main {
    public static String[] FILEDS = new String[]{"字段名", "字段类型", "长度", "主键/外键", "默认值", "描述"};
    public static int[] COLUMN_WIDTHS = new int[]{1504, 1504, 1504, 1504, 1504, 1504};
    public static String[] FILED_NAMES = new String[]{"COLUMN_NAME", "DATA_TYPE", "COLUMN_TYPE", "COLUMN_KEY", "COLUMN_DEFAULT", "COLUMN_COMMENT"};
    public static String DATABASE = "boilerplate";

    public static void main(String[] args) throws Exception {

        Connection conn = JdbcUtils.getConnection();

        String sql = "SELECT table_name, table_type , ENGINE,table_collation,table_comment, create_options FROM information_schema.TABLES WHERE table_schema='" + DATABASE + "'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Map<String, Object> tables = getTableName(rs);

        XWPFDocument xdoc = new XWPFDocument();
        Main xsg_data = new Main();
        for (Map.Entry<String, Object> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            String tableName_CN = String.valueOf(entry.getValue());
            xsg_data.createTable(xdoc, tableName, tableName_CN);
        }
        xsg_data.saveDocument(xdoc, "E:/" + "xsg" + ".docx");
    }

    /**
     * 获取数据库的所有表名及表的信息
     *
     * @param rs
     * @return list
     */
    private static Map<String, Object> getTableName(ResultSet rs) {

//        List<Map<String,String>> list = new ArrayList<Map<String,String>>();
        Map<String, Object> result = new HashMap<>();

        try {
            while (rs.next()) {
                result.put(rs.getString("table_name") + "", rs.getString("table_comment") + "");
//                result.put("table_type", rs.getString("table_type")+"");
//                result.put("engine", rs.getString("engine")+"");
//                result.put("table_collation", rs.getString("table_collation")+"");
//                result.put("table_comment", rs.getString("table_comment")+"");
//                result.put("create_options", rs.getString("create_options")+"");
//                list.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
//        return list;
    }

    /**
     * core code
     *
     * @param xdoc
     * @param tableName
     * @param tableName_CN
     * @throws Exception
     */
    public void createTable(XWPFDocument xdoc, String tableName, String tableName_CN) throws Exception {
        //查数据有多少列
        Connection conn = JdbcUtils.getConnection();
        String sql = "select count(1) from (" + getTableSql(DATABASE, tableName) + ")t";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        int count = 1;
        if (rs.next()) count = rs.getInt(1);

        XWPFParagraph p = xdoc.createParagraph();
        setParagraphSpacingInfo(p, true, "0", "80", null, null, true, "500", STLineSpacingRule.EXACT);
        setParagraphAlignInfo(p, ParagraphAlignment.CENTER, TextAlignment.CENTER);
        XWPFRun pRun = getOrAddParagraphFirstRun(p, false, false);

        /**设置标题头 start*/
        setParagraphRunFontInfo(p, pRun, tableName_CN, "宋体", "Times New Roman", "36", true, false, false, false, null, null, 0, 0, 90);
        p = xdoc.createParagraph();
        setParagraphSpacingInfo(p, true, "0", "80", null, null, true, "500", STLineSpacingRule.EXACT);
        setParagraphAlignInfo(p, ParagraphAlignment.CENTER, TextAlignment.CENTER);
        pRun = getOrAddParagraphFirstRun(p, false, false);
        /**end*/
        setParagraphRunFontInfo(p, pRun, tableName, "宋体", "Times New Roman", "36", true, false, false, false, null, null, 0, 0, 90);
        p = xdoc.createParagraph();
        setParagraphSpacingInfo(p, true, "0", "0", "0", "0", true, "240", STLineSpacingRule.AUTO);
        setParagraphAlignInfo(p, ParagraphAlignment.LEFT, TextAlignment.CENTER);
        pRun = getOrAddParagraphFirstRun(p, false, false);

        // 创建表格21行3列
        XWPFTable table = xdoc.createTable((count == 1 ? 1 : count + 1), FILEDS.length);
        setTableBorders(table, STBorder.SINGLE, "4", "auto", "0");
        setTableWidthAndHAlign(table, "9024", STJc.CENTER);
        setTableCellMargin(table, 0, 108, 0, 108);
        setTableGridCol(table, COLUMN_WIDTHS);

        XWPFTableRow row = table.getRow(0);
        setRowHeight(row, "460", STHeightRule.AT_LEAST);
        XWPFTableCell cell = row.getCell(0);
        setCellShdStyle(cell, true, "FFFFFF", null);
        p = getCellFirstParagraph(cell);
        pRun = getOrAddParagraphFirstRun(p, false, false);

        int index = 0;
        //创建行
        row = table.getRow(index);
        setRowHeight(row, "567", STHeightRule.AT_LEAST);

        //创建列
        for (int i = 0; i < FILEDS.length; i++) {
            cell = row.getCell(i);
            setCellWidthAndVAlign(cell, String.valueOf(COLUMN_WIDTHS[i]), STTblWidth.DXA, STVerticalJc.TOP);
            p = getCellFirstParagraph(cell);
            pRun = getOrAddParagraphFirstRun(p, false, false);
            setParagraphRunFontInfo(p, pRun, FILEDS[i], "宋体", "Times New Roman", "21", false, false, false, false, null, null, 0, 6, 0);
        }

        index = 1;
        //查询数据库表
        sql = getTableSql(DATABASE, tableName);
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            row = table.getRow(index);
            for (int i = 0; i < columnCount; i++) {
                String filedName = FILED_NAMES[i];
                String columnValue = rs.getString(filedName) == null ? "" : rs.getString(FILED_NAMES[i]);
                if ("COLUMN_TYPE".equals(filedName.toUpperCase())) {
                    try {
                        String tmp = columnValue.substring(columnValue.indexOf("(") + 1, columnValue.length() - 1);
                        Integer.parseInt(tmp);
                        columnValue = tmp;
                    } catch (Exception e) {
                        columnValue = columnValue;
                    }
                }
                cell = row.getCell(i);
                setCellWidthAndVAlign(cell, String.valueOf(COLUMN_WIDTHS[i]), STTblWidth.DXA, STVerticalJc.TOP);
                p = getCellFirstParagraph(cell);
                pRun = getOrAddParagraphFirstRun(p, false, false);
                setParagraphRunFontInfo(p, pRun, columnValue, "宋体", "Times New Roman", "21", false, false, false, false, null, null, 0, 6, 0);
            }
            index++;
        }
        System.out.println("创建【" + (StringUtils.isBlank(tableName_CN) ? tableName : tableName_CN) + "】成功！");
    }

    private String getTableSql(String database, String tableName) {
        return "select COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,COLUMN_KEY,COLUMN_DEFAULT,COLUMN_COMMENT from information_schema.columns " +
                "where table_schema = '" + database + "' " +
                "and table_name = '" + tableName + "'";
    }

    /**
     * @Description: 设置Table的边框
     */
    public void setTableBorders(XWPFTable table, STBorder.Enum borderType,
                                String size, String color, String space) {
        CTTblPr tblPr = getTableCTTblPr(table);
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders()
                : tblPr.addNewTblBorders();
        CTBorder hBorder = borders.isSetInsideH() ? borders.getInsideH()
                : borders.addNewInsideH();
        hBorder.setVal(borderType);
        hBorder.setSz(new BigInteger(size));
        hBorder.setColor(color);
        hBorder.setSpace(new BigInteger(space));

        CTBorder vBorder = borders.isSetInsideV() ? borders.getInsideV()
                : borders.addNewInsideV();
        vBorder.setVal(borderType);
        vBorder.setSz(new BigInteger(size));
        vBorder.setColor(color);
        vBorder.setSpace(new BigInteger(space));

        CTBorder lBorder = borders.isSetLeft() ? borders.getLeft() : borders
                .addNewLeft();
        lBorder.setVal(borderType);
        lBorder.setSz(new BigInteger(size));
        lBorder.setColor(color);
        lBorder.setSpace(new BigInteger(space));

        CTBorder rBorder = borders.isSetRight() ? borders.getRight() : borders
                .addNewRight();
        rBorder.setVal(borderType);
        rBorder.setSz(new BigInteger(size));
        rBorder.setColor(color);
        rBorder.setSpace(new BigInteger(space));

        CTBorder tBorder = borders.isSetTop() ? borders.getTop() : borders
                .addNewTop();
        tBorder.setVal(borderType);
        tBorder.setSz(new BigInteger(size));
        tBorder.setColor(color);
        tBorder.setSpace(new BigInteger(space));

        CTBorder bBorder = borders.isSetBottom() ? borders.getBottom()
                : borders.addNewBottom();
        bBorder.setVal(borderType);
        bBorder.setSz(new BigInteger(size));
        bBorder.setColor(color);
        bBorder.setSpace(new BigInteger(space));
    }

    /**
     * @Description: 得到Table的CTTblPr, 不存在则新建
     */
    public CTTblPr getTableCTTblPr(XWPFTable table) {
        CTTbl ttbl = table.getCTTbl();
        CTTblPr tblPr = ttbl.getTblPr() == null ? ttbl.addNewTblPr() : ttbl
                .getTblPr();
        return tblPr;
    }

    /**
     * @Description: 设置表格总宽度与水平对齐方式
     */
    public void setTableWidthAndHAlign(XWPFTable table, String width, STJc.Enum enumValue) {
        CTTblPr tblPr = getTableCTTblPr(table);
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr
                .addNewTblW();
        if (enumValue != null) {
            CTJc cTJc = tblPr.addNewJc();
            cTJc.setVal(enumValue);
        }
        tblWidth.setW(new BigInteger(width));
        tblWidth.setType(STTblWidth.DXA);
    }

    /**
     * @Description: 设置单元格Margin
     */
    public void setTableCellMargin(XWPFTable table, int top, int left,
                                   int bottom, int right) {
        table.setCellMargins(top, left, bottom, right);
    }

    /**
     * @Description: 设置表格列宽
     */
    public void setTableGridCol(XWPFTable table, int[] colWidths) {
        CTTbl ttbl = table.getCTTbl();
        CTTblGrid tblGrid = ttbl.getTblGrid() != null ? ttbl.getTblGrid()
                : ttbl.addNewTblGrid();
        for (int j = 0, len = colWidths.length; j < len; j++) {
            CTTblGridCol gridCol = tblGrid.addNewGridCol();
            gridCol.setW(new BigInteger(String.valueOf(colWidths[j])));
        }
    }

    /**
     * @Description: 设置行高
     */
    public void setRowHeight(XWPFTableRow row, String hight,
                             STHeightRule.Enum heigthEnum) {
        CTTrPr trPr = getRowCTTrPr(row);
        CTHeight trHeight;
        if (trPr.getTrHeightList() != null && trPr.getTrHeightList().size() > 0) {
            trHeight = trPr.getTrHeightList().get(0);
        } else {
            trHeight = trPr.addNewTrHeight();
        }
        trHeight.setVal(new BigInteger(hight));
        if (heigthEnum != null) {
            trHeight.setHRule(heigthEnum);
        }
    }

    /**
     * @Description: 设置底纹
     */
    public void setCellShdStyle(XWPFTableCell cell, boolean isShd,
                                String shdColor, STShd.Enum shdStyle) {
        CTTcPr tcPr = getCellCTTcPr(cell);
        if (isShd) {
            // 设置底纹
            CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
            if (shdStyle != null) {
                shd.setVal(shdStyle);
            }
            if (shdColor != null) {
                shd.setColor(shdColor);
                shd.setFill(shdColor);
            }
        }
    }

    /**
     * @Description: 得到CTTrPr, 不存在则新建
     */
    public CTTrPr getRowCTTrPr(XWPFTableRow row) {
        CTRow ctRow = row.getCtRow();
        CTTrPr trPr = ctRow.isSetTrPr() ? ctRow.getTrPr() : ctRow.addNewTrPr();
        return trPr;
    }

    /**
     * @Description: 得到Cell的CTTcPr, 不存在则新建
     */
    public CTTcPr getCellCTTcPr(XWPFTableCell cell) {
        CTTc cttc = cell.getCTTc();
        CTTcPr tcPr = cttc.isSetTcPr() ? cttc.getTcPr() : cttc.addNewTcPr();
        return tcPr;
    }

    /**
     * @Description: 设置段落间距信息, 一行=100 一磅=20
     */
    public void setParagraphSpacingInfo(XWPFParagraph p, boolean isSpace,
                                        String before, String after, String beforeLines, String afterLines,
                                        boolean isLine, String line, STLineSpacingRule.Enum lineValue) {
        CTPPr pPPr = getParagraphCTPPr(p);
        CTSpacing pSpacing = pPPr.getSpacing() != null ? pPPr.getSpacing()
                : pPPr.addNewSpacing();
        if (isSpace) {
            // 段前磅数
            if (before != null) {
                pSpacing.setBefore(new BigInteger(before));
            }
            // 段后磅数
            if (after != null) {
                pSpacing.setAfter(new BigInteger(after));
            }
            // 段前行数
            if (beforeLines != null) {
                pSpacing.setBeforeLines(new BigInteger(beforeLines));
            }
            // 段后行数
            if (afterLines != null) {
                pSpacing.setAfterLines(new BigInteger(afterLines));
            }
        }
        // 间距
        if (isLine) {
            if (line != null) {
                pSpacing.setLine(new BigInteger(line));
            }
            if (lineValue != null) {
                pSpacing.setLineRule(lineValue);
            }
        }
    }

    /**
     * @Description: 得到段落CTPPr
     */
    public CTPPr getParagraphCTPPr(XWPFParagraph p) {
        CTPPr pPPr = null;
        if (p.getCTP() != null) {
            if (p.getCTP().getPPr() != null) {
                pPPr = p.getCTP().getPPr();
            } else {
                pPPr = p.getCTP().addNewPPr();
            }
        }
        return pPPr;
    }

    /**
     * @Description: 设置段落对齐
     */
    public void setParagraphAlignInfo(XWPFParagraph p,
                                      ParagraphAlignment pAlign, TextAlignment valign) {
        if (pAlign != null) {
            p.setAlignment(pAlign);
        }
        if (valign != null) {
            p.setVerticalAlignment(valign);
        }
    }

    public XWPFRun getOrAddParagraphFirstRun(XWPFParagraph p, boolean isInsert,
                                             boolean isNewLine) {
        XWPFRun pRun = null;
        if (isInsert) {
            pRun = p.createRun();
        } else {
            if (p.getRuns() != null && p.getRuns().size() > 0) {
                pRun = p.getRuns().get(0);
            } else {
                pRun = p.createRun();
            }
        }
        if (isNewLine) {
            pRun.addBreak();
        }
        return pRun;
    }

    /**
     * @Description: 得到单元格第一个Paragraph
     */
    public XWPFParagraph getCellFirstParagraph(XWPFTableCell cell) {
        XWPFParagraph p;
        if (cell.getParagraphs() != null && cell.getParagraphs().size() > 0) {
            p = cell.getParagraphs().get(0);
        } else {
            p = cell.addParagraph();
        }
        return p;
    }

    /**
     * @Description: 设置列宽和垂直对齐方式
     */
    public void setCellWidthAndVAlign(XWPFTableCell cell, String width,
                                      STTblWidth.Enum typeEnum, STVerticalJc.Enum vAlign) {
        CTTcPr tcPr = getCellCTTcPr(cell);
        CTTblWidth tcw = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        if (width != null) {
            tcw.setW(new BigInteger(width));
        }
        if (typeEnum != null) {
            tcw.setType(typeEnum);
        }
        if (vAlign != null) {
            CTVerticalJc vJc = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr
                    .addNewVAlign();
            vJc.setVal(vAlign);
        }
    }

    /**
     * @param verticalAlign : SUPERSCRIPT上标 SUBSCRIPT下标
     * @param position      :字符间距位置：>0提升 <0降低=磅值*2 如3磅=6
     * @param spacingValue  :字符间距间距 >0加宽 <0紧缩 =磅值*20 如2磅=40
     * @param indent        :字符间距缩进 <100 缩
     * @Description: 设置段落文本样式(高亮与底纹显示效果不同)设置字符间距信息(CTSignedTwipsMeasure)
     */

    public void setParagraphRunFontInfo(XWPFParagraph p, XWPFRun pRun,
                                        String content, String cnFontFamily, String enFontFamily,
                                        String fontSize, boolean isBlod, boolean isItalic,
                                        boolean isStrike, boolean isShd, String shdColor,
                                        STShd.Enum shdStyle, int position, int spacingValue, int indent) {
        CTRPr pRpr = getRunCTRPr(p, pRun);
        if (StringUtils.isNotBlank(content)) {
            // pRun.setText(content);
            if (content.contains("\n")) {// System.properties("line.separator")
                String[] lines = content.split("\n");
                pRun.setText(lines[0], 0); // set first line into XWPFRun
                for (int i = 1; i < lines.length; i++) {
                    // add break and insert new text
                    pRun.addBreak();
                    pRun.setText(lines[i]);
                }
            } else {
                pRun.setText(content, 0);
            }
        }
        // 设置字体
        CTFonts fonts = pRpr.isSetRFonts() ? pRpr.getRFonts() : pRpr
                .addNewRFonts();
        if (StringUtils.isNotBlank(enFontFamily)) {
            fonts.setAscii(enFontFamily);
            fonts.setHAnsi(enFontFamily);
        }
        if (StringUtils.isNotBlank(cnFontFamily)) {
            fonts.setEastAsia(cnFontFamily);
            fonts.setHint(STHint.EAST_ASIA);
        }
        // 设置字体大小
        CTHpsMeasure sz = pRpr.isSetSz() ? pRpr.getSz() : pRpr.addNewSz();
        sz.setVal(new BigInteger(fontSize));

        CTHpsMeasure szCs = pRpr.isSetSzCs() ? pRpr.getSzCs() : pRpr
                .addNewSzCs();
        szCs.setVal(new BigInteger(fontSize));

        // 设置字体样式
        // 加粗
        if (isBlod) {
            pRun.setBold(isBlod);
        }
        // 倾斜
        if (isItalic) {
            pRun.setItalic(isItalic);
        }
        // 删除线
        if (isStrike) {
            pRun.setStrike(isStrike);
        }
        if (isShd) {
            // 设置底纹
            CTShd shd = pRpr.isSetShd() ? pRpr.getShd() : pRpr.addNewShd();
            if (shdStyle != null) {
                shd.setVal(shdStyle);
            }
            if (shdColor != null) {
                shd.setColor(shdColor);
                shd.setFill(shdColor);
            }
        }

        // 设置文本位置
        if (position != 0) {
            pRun.setTextPosition(position);
        }
        if (spacingValue > 0) {
            // 设置字符间距信息
            CTSignedTwipsMeasure ctSTwipsMeasure = pRpr.isSetSpacing() ? pRpr
                    .getSpacing() : pRpr.addNewSpacing();
            ctSTwipsMeasure
                    .setVal(new BigInteger(String.valueOf(spacingValue)));
        }
        if (indent > 0) {
            CTTextScale paramCTTextScale = pRpr.isSetW() ? pRpr.getW() : pRpr
                    .addNewW();
            paramCTTextScale.setVal(indent);
        }
    }

    /**
     * @Description: 跨列合并
     */
    public void mergeCellsHorizontal(XWPFTable table, int row, int fromCell,
                                     int toCell) {
        for (int cellIndex = fromCell; cellIndex <= toCell; cellIndex++) {
            XWPFTableCell cell = table.getRow(row).getCell(cellIndex);
            if (cellIndex == fromCell) {
                // The first merged cell is set with RESTART merge value
                getCellCTTcPr(cell).addNewHMerge().setVal(STMerge.RESTART);
            } else {
                // Cells which join (merge) the first one,are set with CONTINUE
                getCellCTTcPr(cell).addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    /**
     * @Description: 得到XWPFRun的CTRPr
     */
    public CTRPr getRunCTRPr(XWPFParagraph p, XWPFRun pRun) {
        CTRPr pRpr = null;
        if (pRun.getCTR() != null) {
            pRpr = pRun.getCTR().getRPr();
            if (pRpr == null) {
                pRpr = pRun.getCTR().addNewRPr();
            }
        } else {
            pRpr = p.getCTP().addNewR().addNewRPr();
        }
        return pRpr;
    }

    /**
     * @Description: 保存文档
     */
    public void saveDocument(XWPFDocument document, String savePath)
            throws Exception {
        FileOutputStream fos = new FileOutputStream(savePath);
        document.write(fos);
        fos.close();
    }
}
