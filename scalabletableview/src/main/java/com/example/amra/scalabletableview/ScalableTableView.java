package com.example.amra.scalabletableview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

public class ScalableTableView extends GesturesView {

    private final static String LOGTAG = "TABLEVIEW";

    // Границы и размеры основого вида таблицы (без боковиков и заголовков)
    int mainTableLeft;
    int mainTableRight;
    int mainTableTop;
    int mainTableBottom;
    int mainTableWidth;
    int mainTableHeight;

    // Количество столбцов и строк
    int columns = 3;
    int rows = 3;

    // Размеры ячекйки при масштабе 1:1
    int cellWidth = 100;
    int cellHeight = 100;

    // Видимые размеры ячейки
    float visualCellHeight;
    float visualCellWidth;


    // Высота шапки
    int headerHeight = 50;
    // Ширина боковика
    int headerWidth = 50;

    private Adapter adapter;

    float startX = 0;
    float startY = 0;
    float leftXLimit, rightXLimit, topYLimit, bottomYLimit;
    float currentX = startX;
    float currentY = startY;

    float startScale = 1;
    float minScale = 0.33f;
    float maxScale = 4;
    float currentScale = startScale;

    int visualWidth, visualHeight;

    int bgColor = Color.parseColor("#333333");

    Paint paintBG;
    Paint paintMainTable;
    Paint paintMainTablePressed;
    Paint paintText;
    Paint paintHeader;

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public void setCellHeight(int cellHeight) {
        this.cellHeight = cellHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    public void setHeaderWidth(int headerWidth) {
        this.headerWidth = headerWidth;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public ScalableTableView(Context context) {
        super(context);
        Log.i(LOGTAG, "Constructor 1");
        init();
    }

    public ScalableTableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.i(LOGTAG, "Constructor 2");
        init();
    }

    public ScalableTableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.i(LOGTAG, "Constructor 3");
        init();
    }

    private void init() {

        paintBG = new Paint();
        paintBG.setColor(bgColor);

        paintText = new Paint(); // Outside the circle
        paintText.setAntiAlias(true);
        paintText.setColor(Color.parseColor("#33ffffff"));
        paintText.setStyle(Paint.Style.FILL);
        paintText.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(30);


        paintHeader = new Paint();
        paintHeader.setColor(Color.parseColor("#22ffffff"));

        paintMainTable = new Paint();
        paintMainTable.setColor(Color.parseColor("#33ffffff"));

        paintMainTablePressed = new Paint();
        paintMainTablePressed.setColor(Color.parseColor("#66ffffff"));

        // Демо-адаптер
        adapter = new Adapter() {
            @Override
            void drawCell(Canvas canvas, int column, int row) {
                if (pressedCellColumn != null && column == pressedCellColumn && row == pressedCellRow) {
                    canvas.drawRect(0, 0, cellWidth - 1, cellHeight - 1, paintMainTablePressed);
                } else {
                    canvas.drawRect(0, 0, cellWidth - 1, cellHeight - 1, paintMainTable);
                }

                canvas.drawText(column + "," + row, 4, cellHeight - 4, paintText);
            }

            @Override
            void drawHeaderTop(Canvas canvas, int column) {
                canvas.drawRect(0, 0, cellWidth - 1, headerHeight - 1, paintHeader);
                canvas.drawText(column + "", 4, headerHeight - 4, paintText);
            }

            @Override
            void drawHeaderLeft(Canvas canvas, int row) {
                canvas.drawRect(0, 0, headerWidth - 1, cellHeight - 1, paintHeader);
                canvas.drawText(row + "", 4, cellHeight - 4, paintText);
            }
        };
        adapter.setCellsSizes(50, 50, 50, 50);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        calculateMainTableViewPort();
        calculateDragLimits();
        checkLimits();

        visualCellWidth = cellWidth * currentScale;
        visualCellHeight = cellHeight * currentScale;

        // Устанавливаем размеры ячеек
        adapter.setCellsSizes(visualCellWidth, visualCellHeight, headerWidth, headerHeight);

        // Определяем, какие ячейки попадают во вьюпорт
        // Для этого определим минимальные и максимальные индексы столбцов и строк, попадающих во вьюпорт
        int columnFirstVisible, columnLastVisible;
        if (visualWidth <= mainTableWidth) {
            columnFirstVisible = 0;
            columnLastVisible = columns - 1;
        } else {
            columnFirstVisible = (int) ((-currentX + mainTableLeft) / visualCellWidth);
            columnLastVisible = Math.abs((int) ((currentX - mainTableRight) / visualCellWidth));
        }
        int rowFirstVisible, rowLastVisible;
        if (visualHeight <= mainTableHeight) {
            rowFirstVisible = 0;
            rowLastVisible = rows - 1;
        } else {
            rowFirstVisible = (int) ((-currentY + mainTableTop) / visualCellHeight);
            rowLastVisible = Math.abs((int) ((currentY - mainTableBottom) / visualCellHeight));
        }

        // Общий фон
        canvas.drawRect(0, 0, getWidth(), getHeight(), paintBG);

        // Пробегаем по видимым ячейкам и рисуем их
        for (int i = columnFirstVisible; i <= columnLastVisible; i++) {
            for (int j = rowFirstVisible; j <= rowLastVisible; j++) {
                canvas.translate(currentX + i * visualCellWidth, currentY + j * visualCellHeight);
                // Фон ячейки, чтобы закрыть текст предыдущих ячеек, если он накладывается на текущую.
                canvas.drawRect(0, 0, visualCellWidth, visualCellHeight, paintBG);
                adapter.drawCell(canvas, i, j);
                canvas.translate(-(currentX + i * visualCellWidth), -(currentY + j * visualCellHeight));
            }
        }

        // Фон для шапки
        canvas.drawRect(mainTableLeft, 0, mainTableRight, mainTableTop, paintBG);

        // Ячейки шапки
        canvas.drawRect(mainTableLeft, 0, mainTableRight, mainTableTop, paintBG);
        for (int i = columnFirstVisible; i <= columnLastVisible; i++) {
            canvas.translate(currentX + i * cellWidth * currentScale, 0);
            // Фон ячейки, чтобы закрыть текст предыдущих ячеек, если он накладывается на текущую.
            canvas.drawRect(0, 0, visualCellWidth, headerHeight, paintBG);
            adapter.drawHeaderTop(canvas, i);
            canvas.translate(-currentX - i * cellWidth * currentScale, 0);
        }

        // Фон для боковика
        canvas.drawRect(0, mainTableTop, mainTableLeft, mainTableBottom, paintBG);

        // Ячейки боковика
        canvas.drawRect(0, mainTableTop, mainTableLeft, mainTableBottom, paintBG);
        for (int j = rowFirstVisible; j <= rowLastVisible; j++) {
            canvas.translate(0, currentY + j * cellHeight * currentScale);
            // Фон ячейки, чтобы закрыть текст предыдущих ячеек, если он накладывается на текущую.
            canvas.drawRect(0, 0, headerWidth, visualCellHeight, paintBG);
            adapter.drawHeaderLeft(canvas, j);
            canvas.translate(0, -currentY - j * cellHeight * currentScale);
        }

        // Левый верхний угол
        canvas.drawRect(0, 0, mainTableLeft, mainTableTop, paintBG);


    }

    private void calculateMainTableViewPort() {
        mainTableLeft = headerWidth;
        mainTableRight = getWidth();
        mainTableTop = headerHeight;
        mainTableBottom = getHeight();
        mainTableWidth = mainTableRight - mainTableLeft;
        mainTableHeight = mainTableBottom - mainTableTop;
    }

    private void calculateDragLimits() {
        visualWidth = (int) (columns * cellWidth * currentScale);
        visualHeight = (int) (rows * cellHeight * currentScale);

        leftXLimit = visualWidth > mainTableWidth ? mainTableRight - visualWidth : mainTableLeft + (mainTableWidth - visualWidth) / 2;
        rightXLimit = visualWidth > mainTableWidth ? mainTableLeft : mainTableLeft + (mainTableWidth - visualWidth) / 2;
        topYLimit = visualHeight > mainTableHeight ? mainTableBottom - visualHeight : mainTableTop + (mainTableHeight - visualHeight) / 2;
        bottomYLimit = visualHeight > mainTableHeight ? mainTableTop : mainTableTop + (mainTableHeight - visualHeight) / 2;
    }

    private void checkLimits() {
        if (currentX < leftXLimit) currentX = leftXLimit;
        else if (currentX > rightXLimit) currentX = rightXLimit;
        if (currentY < topYLimit) currentY = topYLimit;
        else if (currentY > bottomYLimit) currentY = bottomYLimit;
    }

    @Override
    void onPress(float x, float y) {
        Log.i(LOGTAG, "onPress");
        // Вычисляем столбец-строку нажатой ячейки
        int column = (int) ((-currentX + x) / visualCellWidth);
        int row = (int) ((-currentY + y) / visualCellHeight);

        if (column < 0 || column > columns || row < 0 || row > rows) {
            // нажатие на фон шапку или боковик
        } else {
            // нажатие на ячейку
            adapter.setPressedCell(column, row);
            invalidate();
        }
    }

    @Override
    void onLongPress(float x, float y) {
        Log.i(LOGTAG, "onLongPress");
        adapter.onLongPressPerformed();
        adapter.clearPressedCell();
        invalidate();
    }

    @Override
    void onClick(float x, float y) {
        Log.i(LOGTAG, "onClick");
        //adapter.onClickPerformed();
        adapter.clearPressedCell();
        invalidate();
    }

    @Override
    void onDrag(float dx, float dy) {
        adapter.clearPressedCell();
        currentX += dx;
        currentY += dy;
        invalidate();
    }

    @Override
    void onPinch(float dx, float dy, float anchorX, float anchorY, float scaleStep) {
        adapter.clearPressedCell();
        currentX += dx;
        currentY += dy;

        currentScale = currentScale * scaleStep;
        if (currentScale < minScale) currentScale = minScale;
        else if (currentScale > maxScale) currentScale = maxScale;

        Log.i(LOGTAG, "currentScale " + currentScale);

        float leftPart = anchorX - currentX;
        float scaledLeftPart = leftPart * scaleStep;
        float afterScalingPositionX = anchorX - scaledLeftPart;
        float dx1 = afterScalingPositionX - currentX;

        float topPart = anchorY - currentY;
        float scaledTopPart = topPart * scaleStep;
        float afterScalingPositionY = anchorY - scaledTopPart;
        float dy1 = afterScalingPositionY - currentY;

        currentX += dx1;
        currentY += dy1;

        invalidate();
    }

    public abstract static class Adapter {

        float cellWidth, cellHeight, headerWidth, headerHeight = 0;

        Integer pressedCellColumn, pressedCellRow;

        public void setCellsSizes(float cellWidth, float cellHeight, float headerWidth, float headerHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            this.headerWidth = headerWidth;
            this.headerHeight = headerHeight;
        }

        /**
         * Установить нажатую ячейку
         */
        public void setPressedCell(Integer pressedCellColumn, Integer pressedCellRow) {
            this.pressedCellColumn = pressedCellColumn;
            this.pressedCellRow = pressedCellRow;
        }

        /**
         * Сбросить нажатую ячейку
         */
        public void clearPressedCell() {
            pressedCellColumn = null;
            pressedCellRow = null;
        }

        public void onLongPressPerformed(){
            if (pressedCellColumn!=null) {
                // getObject
            }
        }

        // Нарисовать ячейку таблицы
        abstract void drawCell(Canvas canvas, int column, int row);

        // Нарисовать ячейку шапки
        abstract void drawHeaderTop(Canvas canvas, int column);

        // Нарисовать ячейку боковика
        abstract void drawHeaderLeft(Canvas canvas, int row);
    }


}
