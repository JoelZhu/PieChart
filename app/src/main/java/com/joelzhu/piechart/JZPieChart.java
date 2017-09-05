package com.joelzhu.piechart;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：JoelZhu
 * 时间：2017年01月10日 14:00
 * 作用：JoelZhuPieChart
 */
public class JZPieChart extends View {
    // 栏目对象点击事件
    private OnColumnClickListener listener;

    // 当前最大角度
    private int currentMaxDegrees;

    // Paint对象
    private Paint paint;
    // Rect对象
    private Rect rect;
    // RectF对象
    private RectF rectF;
    // 绘制Region用Path对象
    private Path path;
    // 所有栏目的Region对象数组
    private Region[] regions;

    // 参数区域单位高度
    private int textUnitHeight;

    // 栏目
    private String[] columns;
    // 栏目权重
    private int[] weights;
    // 栏目颜色
    private int[] colors;
    // 栏目百分比
    private List<Float> columnsPercent;
    // 栏目总数
    private int columnsLength;

    // 当前着重显示的栏目
    private int currentAccent = -1;
    // 上一次动画是否执行完毕
    private boolean isLastAnimatorFinished = false;

    public JZPieChart(Context context) {
        super(context);

        // 初始化控件
        initWidget();
    }

    public JZPieChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 初始化控件
        initWidget();
    }

    public JZPieChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化控件
        initWidget();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;
        // 判断宽度指定模式
        if (widthMode == MeasureSpec.EXACTLY) {
            // 引用指定宽度
            width = widthSize;
        } else {
            // 默认控件宽度设置为400dp
            width = dp2Px(400);
        }

        // 判断高度指定模式
        if (heightMode == MeasureSpec.EXACTLY) {
            // 引用指定高度
            height = heightSize;
        }
        // 宽度精确指定，高度没有指定
        else if (widthMode == MeasureSpec.EXACTLY) {
            // 按照宽的八分之五作为默认高
            height = (int) (width * 0.625);
        } else {
            // 默认控件高度设置为250dp
            height = dp2Px(250);
        }

        // 计算参数区域单位高度(每个单位区域分成4部分，上面和下面间隔1个单位，绘制2个单位)
        textUnitHeight = (height * 7 / 8) / columnsLength / 4;
        // 初始化PieChart位置
        rectF.set(height / 16, height / 16, height * 15 / 16, height * 15 / 16);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制PieChart
        drawPieChartWithMaxDegrees(canvas, currentMaxDegrees);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (regions != null && listener != null && isLastAnimatorFinished) {
                final int size = regions.length;
                // 遍历Region数组
                for (int i = 0; i < size; i++) {
                    // 点击区域是栏目中的一个，输出事件
                    if (regions[i].contains((int) event.getX(), (int) event.getY())) {
                        // 标志位置否
                        isLastAnimatorFinished = false;
                        // 如果当前没有着重栏目，进行缩放着重
                        if (currentAccent == -1) {
                            listener.onColumnClick(i);
                            currentAccent = i;
                            // 着重栏目显示
                            accentPieChart();
                        }
                        // 如果当前有着重栏目，进行扩展恢复正常状态
                        else {
                            // 恢复饼图正常显示
                            normalPieChart();
                        }

                        // 事件处理完成，停止回馈上级
                        return true;
                    }
                }
            }
        }

        return super.onTouchEvent(event);
    }

    /**
     * 初始化控件
     */
    private void initWidget() {
        // 执行绘制控件
        setWillNotDraw(false);

        // 初始化Paint对象
        paint = new Paint();
        // 初始化Rect
        rect = new Rect();
        // 初始化RectF对象
        rectF = new RectF();
        // 初始化Path对象
        path = new Path();

        // 初始化栏目百分比
        columnsPercent = new ArrayList<>();
    }

    /**
     * 按照最大角度绘制PieChart
     *
     * @param canvas     Canvas对象
     * @param maxDegrees 最大角度
     */
    private void drawPieChartWithMaxDegrees(Canvas canvas, int maxDegrees) {
        // 初始圆弧角度
        float degrees = -90;
        // 遍历数组，绘制圆弧
        for (int i = 0; i < columnsLength; i++) {
            // 判断栏目权重是否为0
            final float deltaD = currentAccent == i ? columnsPercent.get(i) * 360 : columnsPercent.get(i) * maxDegrees;
            if (columnsPercent.get(i) != 0 && deltaD != 0) {
                // 如果栏目权重不为0，进行绘制
                paint.reset();
                paint.setAntiAlias(true);
                paint.setColor(ContextCompat.getColor(getContext(), colors[i]));
                canvas.drawArc(rectF, degrees, deltaD, true, paint);

                // 如果饼图绘制完成(动画效果完成)，计算各栏目的区域
                if (maxDegrees == 360 && regions[i] == null) {
                    // 计算区域边界的Path
                    path.reset();
                    path.moveTo(rectF.centerX(), rectF.centerY());
                    path.lineTo(rectF.centerX() + (float) Math.sin(degrees),
                            rectF.centerY() + (float) Math.cos(degrees));
                    path.arcTo(rectF, degrees, deltaD);
                    // 计算Region对象
                    Region region = new Region();
                    region.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF
                            .bottom));
                    // 添加到数组中
                    regions[i] = region;
                }
                degrees = degrees + deltaD;
            }

            // 左 - PieChart宽度加上一个单位高度
            final int left = getMeasuredHeight() + textUnitHeight;
            // 上 - PieChart间距加上一个单位高度加上不同项目之间的偏移量
            final int top = getMeasuredHeight() / 16 + textUnitHeight + i * 4 * textUnitHeight;
            // 右 - PieChart宽度加上三个单位高度
            final int right = getMeasuredHeight() + 3 * textUnitHeight;
            // 底 - PieChart间距加上三个单位高度加上不同项目之间的偏移量
            final int bottom = getMeasuredHeight() / 16 + 3 * textUnitHeight + i * 4 * textUnitHeight;
            // 绘制右侧参数区域示例图标
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(ContextCompat.getColor(getContext(), colors[i]));
            rect.set(left, top, right, bottom);
            canvas.drawRect(rect, paint);

            // 绘制右侧参数区域说明文字
            final String text = String.format("%s (%s)", columns[i], floatToPercent(columnsPercent.get(i), 1, true));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(Color.rgb(37, 37, 37));
            paint.setTextSize(dp2Px(14));
            rect.set(left + 4 * textUnitHeight, top, getMeasuredWidth(), bottom);
            paint.getTextBounds(text, 0, text.length(), rect);
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            // 计算文本居左时的XY坐标
            float xPosition = left + 3 * textUnitHeight;
            float yPosition = (2 * textUnitHeight - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top + top;
            canvas.drawText(text, xPosition, yPosition, paint);
        }
    }

    /**
     * 初始化PieChart
     *
     * @param columns 栏目
     * @param weights 栏目权重
     * @param colors  栏目颜色
     */
    public void initPieChart(String[] columns, int[] weights, int[] colors) {
        // 合法性检查
        if (columns.length != colors.length || colors.length != weights.length) {
            throw new RuntimeException("PieCHart's columns doesn't match they colors.");
        }
        // 初始化参数
        else {
            this.columns = columns;
            this.weights = weights;
            this.colors = colors;

            // 计算栏目Length
            columnsLength = columns.length;

            // 计算栏目的总计权重
            float sum = 0;
            for (int weight : weights) {
                sum = sum + weight;
            }
            // 计算每个栏目的百分比
            for (int weight : weights) {
                columnsPercent.add(weight / sum);
            }

            // 分配Region数组空间
            regions = new Region[columnsLength];
        }
    }

    /**
     * PieChart是否初始化完成
     *
     * @return true - 初始化完成；false - 初始化未完成
     */
    public boolean isInitFinished() {
        return !(columns == null || columns.length == 0 ||
                weights == null || weights.length == 0 ||
                colors == null || colors.length == 0);
    }

    /**
     * 绘制没有动画效果的PieChart
     */
    public void drawPieChartWithoutAnimation() {
        if (isInitFinished()) {
            // 按照360度绘制PieChart
            currentMaxDegrees = 360;
            invalidate();
        } else {
            throw new RuntimeException("PieChart must been initialized before draw.");
        }
    }

    /**
     * 绘制带有动画效果的PieChart
     */
    public void drawPieChartWithAnimation() {
        if (isInitFinished()) {
            if (currentMaxDegrees == 0) {
                normalPieChart();
            }
        } else {
            throw new RuntimeException("PieChart must been initialized before draw.");
        }
    }

    /**
     * 着重栏目显示
     */
    private void accentPieChart() {
        ValueAnimator animator = ValueAnimator.ofInt(360, 0);
        animator.setDuration(1000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                currentMaxDegrees = (int) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isLastAnimatorFinished = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    /**
     * 恢复饼图正常显示
     */
    private void normalPieChart() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 360);
        animator.setDuration(1000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                currentMaxDegrees = (int) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentAccent = -1;
                isLastAnimatorFinished = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    /**
     * 将DP单位的值转成为PX单位的值
     *
     * @param dpValue 换算前的DP值
     * @return 换算后的PX值
     */
    private int dp2Px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 浮点型保留小数格式化
     *
     * @param srcFloat 原浮点型
     * @param scale    小数位数
     * @return 转化完成之后的字符串
     */
    private String floatToPercent(float srcFloat, int scale, boolean isPercent) {
        // 计算格式化类型
        StringBuilder sb = new StringBuilder("#.");
        for (int i = 1; i <= scale; i++) {
            sb.append("0");
        }

        // 返回值
        String returnResult = String.valueOf(
                new DecimalFormat(sb.toString()).format(isPercent ? srcFloat * 100 : srcFloat));
        // 返回值是否小于零
        if (returnResult.startsWith(".")) {
            // 结果小于零
            returnResult = "0" + returnResult;
        }

        // 是否需要加上百分号
        if (isPercent) {
            // 需要加上百分号
            returnResult = returnResult + "%";
        }

        return returnResult;
    }

    /**
     * 栏目对象点击事件
     */
    interface OnColumnClickListener {
        void onColumnClick(int index);
    }

    /**
     * 设置点击事件监听器
     *
     * @param listener 点击事件监听器
     */
    public void setOnColumnClickListener(OnColumnClickListener listener) {
        this.listener = listener;
    }
}