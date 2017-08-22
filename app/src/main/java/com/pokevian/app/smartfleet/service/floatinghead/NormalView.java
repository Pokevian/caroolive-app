/*
 * Copyright (c) 2015. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.service.floatinghead;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokevian.lib.obd2.data.ObdData;

import java.text.DecimalFormat;

/**
 * Created by dg.kim on 2015-03-20.
 */
public class NormalView extends FrameLayout {

    private static final int COLOR_BG = 0xffffffff;
    private static final int COLOR_DRIVING_BORDER = 0xffcbcbcb;
    private static final int COLOR_IDLING_BORDER = 0xff0eb4dd;
    private static final int COLOR_GRAY = 0xff5e5e5e;
    private static final int COLOR_BLUE = 0xff0eb4dd;
    private static final int COLOR_YELLOW = 0xfff8d532;
    private static final int COLOR_ORANGE = 0xfff89942;
    private static final int COLOR_RED = 0xfff16863;
    private static final int COLOR_GREEN = 0xff94ca53;
    private static final int COLOR_INDIGO = 0xff4581c3;

    private static final float THIN_BORDER_THICKNESS_DP = 1;
    private static final float THICK_BORDER_THICKNESS_DP = 4;

    private static final int MAX_SPEED = 200; // km/h

    public static final int EVENT_MIL = 0x00000001; // not used here
    public static final int EVENT_FUEL_CUT = 0x00000002;
    public static final int EVENT_HARSH_ACCEL = 0x00000004;
    public static final int EVENT_HARSH_BRAKE = 0x00000008;
    public static final int EVENT_IDLING_STEP1 = 0x00000010;
    public static final int EVENT_IDLING_STEP2 = 0x00000020;
    public static final int EVENT_IDLING_STEP3 = 0x00000040;
    public static final int EVENT_OVERSPEED_STEP1 = 0x00000080;
    public static final int EVENT_OVERSPEED_STEP2 = 0x00000100;
    public static final int EVENT_OVERSPEED_STEP3 = 0x00000200;
    public static final int EVENT_ECO_SPEED = 0x00000400;

    private Paint mBgPaint;
    private Paint mThinBorderPaint;
    private Paint mThickBorderPaint;

    private final RectF mBgBounds = new RectF();
    private final RectF mThinBorderBounds = new RectF();
    private final RectF mThickBorderBounds = new RectF();

    private TextView mTitleText;
    private ImageView mEventImage;
    private View mValuePane;
    private TextView mValueText;
    private TextView mUnitText;
    private ImageView mHarshImage;
    private final DecimalFormat mSpeedFormatter = new DecimalFormat("###");
    private final DecimalFormat mFuelEconomyFormatter = new DecimalFormat("#0.0");

//    private int mSpeed;
//    private Unit mSpeedUnit;
//    private int mEcoLevel;
//    private float mFuelEconomy;
//    private Unit mFuelEconomyUnit;
//    private int mEventMask;
    private boolean mFuelCutContinue;
    private boolean mEcoSpeedContinue;

    public NormalView(Context context) {
        this(context, null);
    }

    public NormalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NormalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setColor(COLOR_BG);

        /*mThinBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThinBorderPaint.setStyle(Paint.Style.STROKE);
        float thickness = Utils.dp2px(getResources(), THIN_BORDER_THICKNESS_DP);
        mThinBorderPaint.setStrokeWidth(thickness);
        mThinBorderPaint.setStrokeCap(Paint.Cap.ROUND);

        mThickBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThickBorderPaint.setStyle(Paint.Style.STROKE);
        thickness = Utils.dp2px(getResources(), THICK_BORDER_THICKNESS_DP);
        mThickBorderPaint.setStrokeWidth(thickness);
        mThickBorderPaint.setStrokeCap(Paint.Cap.ROUND);

        SettingsStore settingsStore = SettingsStore.getInstance();
        mSpeedUnit = settingsStore.getSpeedUnit();
        mFuelEconomyUnit = settingsStore.getFuelEconomyUnit();*/
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        /*mTitleText = (TextView) findViewById(R.id.title);
        mEventImage = (ImageView) findViewById(R.id.event);

        mValuePane = findViewById(R.id.value_pane);
        mValueText = (TextView) findViewById(R.id.value);
        mUnitText = (TextView) findViewById(R.id.unit);
        mHarshImage = (ImageView) findViewById(R.id.harsh);*/

        /*FontManager fm = FontManager.getInstance();
        fm.applyFont(this);*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //Logger.getLogger(TAG).error("# onSizeChanged(): " + String.format("%d x %d", w, h));

        Rect bounds = new Rect(0, 0, w, h);

        /*Resources res = getResources();
        mBgBounds.set(bounds.left + Utils.dp2px(res, 4),
                bounds.top + Utils.dp2px(res, 4),
                bounds.right - Utils.dp2px(res, 4),
                bounds.bottom - Utils.dp2px(res, 4));

        mThinBorderBounds.set(mBgBounds);
        float inset = Utils.dp2px(res, THIN_BORDER_THICKNESS_DP / 2) + 1;
        mThinBorderBounds.inset(inset, inset);

        mThickBorderBounds.set(mBgBounds);
        inset = Utils.dp2px(res, THICK_BORDER_THICKNESS_DP / 2) + 1;
        mThickBorderBounds.inset(inset, inset);*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // background bg
        canvas.drawArc(mBgBounds, getStartAngle(), 360, true, mBgPaint);

        /*if ((mEventMask & EVENT_HARSH_ACCEL) != 0) {
            // harsh accel
            mTitleText.setVisibility(View.VISIBLE);
            mTitleText.setText(R.string.harsh_accel);
            mEventImage.setVisibility(View.INVISIBLE);
            mValuePane.setVisibility(INVISIBLE);
            mHarshImage.setVisibility(View.VISIBLE);
            mHarshImage.setImageResource(R.drawable.ic_floating_harsh_accel);
            drawThickBorder(canvas, COLOR_RED);
        } else if ((mEventMask & EVENT_HARSH_BRAKE) != 0) {
            // harsh brake
            mTitleText.setVisibility(View.VISIBLE);
            mTitleText.setText(R.string.harsh_brake);
            mEventImage.setVisibility(View.INVISIBLE);
            mValuePane.setVisibility(INVISIBLE);
            mHarshImage.setVisibility(View.VISIBLE);
            mHarshImage.setImageResource(R.drawable.ic_floating_harsh_brake);
            drawThickBorder(canvas, COLOR_RED);
        } else {
            if (mEventMask != 0) {
                mTitleText.setVisibility(View.INVISIBLE);
                mEventImage.setVisibility(View.VISIBLE);
            } else {
                mTitleText.setVisibility(View.VISIBLE);
                mEventImage.setVisibility(View.INVISIBLE);
            }
            mValuePane.setVisibility(View.VISIBLE);
            mHarshImage.setVisibility(View.INVISIBLE);

            // driving or not
            if (mSpeed > 0) {
                mTitleText.setText(R.string.driving_speed);
                mValueText.setText(mSpeedFormatter.format(mSpeed));
                mUnitText.setText(mSpeedUnit.toString());

                drawThinBorder(canvas, COLOR_DRIVING_BORDER);
                mThickBorderPaint.setColor(getEcoColor(mEcoLevel));
                canvas.drawArc(mThickBorderBounds, getStartAngle(), getSpeedAngle(mSpeed), false, mThickBorderPaint);
            } else {
                mValuePane.setVisibility(View.VISIBLE);
                mTitleText.setText(R.string.fuel_economy);
                mValueText.setText(mFuelEconomyFormatter.format(mFuelEconomy));
                mUnitText.setText(mFuelEconomyUnit.toString());

                drawThinBorder(canvas, COLOR_IDLING_BORDER);
            }

            // keep order
            if ((mEventMask & EVENT_FUEL_CUT) != 0) {
                // fuel-cut
                mEventImage.setImageResource(R.drawable.ic_floating_fuel_cut);
                if (!mFuelCutContinue) {
                    drawThickBorder(canvas, COLOR_INDIGO);
                }
            } else if ((mEventMask & EVENT_ECO_SPEED) != 0) {
                // eco speed
                mEventImage.setImageResource(R.drawable.ic_floating_eco);
                if (!mEcoSpeedContinue) {
                    drawThickBorder(canvas, COLOR_GREEN);
                }
            } else if ((mEventMask & EVENT_IDLING_STEP3) != 0) {
                // idling step-3
                mEventImage.setImageResource(R.drawable.ic_floating_idling_step3);
                drawThickBorder(canvas, COLOR_RED);
            } else if ((mEventMask & EVENT_IDLING_STEP2) != 0) {
                // idling step-2
                mEventImage.setImageResource(R.drawable.ic_floating_idling_step2);
                drawThickBorder(canvas, COLOR_ORANGE);
            } else if ((mEventMask & EVENT_IDLING_STEP1) != 0) {
                // idling step-1
                mEventImage.setImageResource(R.drawable.ic_floating_idling_step1);
                drawThickBorder(canvas, COLOR_YELLOW);
            } else if ((mEventMask & EVENT_OVERSPEED_STEP3) != 0) {
                // over-speed step-3
                mEventImage.setImageResource(R.drawable.ic_floating_overspeed_step3);
                drawThickBorder(canvas, COLOR_RED);
            } else if ((mEventMask & EVENT_OVERSPEED_STEP2) != 0) {
                // over-speed step-2
                mEventImage.setImageResource(R.drawable.ic_floating_overspeed_step2);
                drawThickBorder(canvas, COLOR_ORANGE);
            } else if ((mEventMask & EVENT_OVERSPEED_STEP1) != 0) {
                // over-speed step-1
                mEventImage.setImageResource(R.drawable.ic_floating_overspeed_step1);
                drawThickBorder(canvas, COLOR_YELLOW);
            }
        }*/
    }

    private void drawThinBorder(Canvas canvas, int color) {
        mThinBorderPaint.setColor(color);
        canvas.drawArc(mThinBorderBounds, getStartAngle(), 360, false, mThinBorderPaint);
    }

    private void drawThickBorder(Canvas canvas, int color) {
        mThickBorderPaint.setColor(color);
        canvas.drawArc(mThickBorderBounds, getStartAngle(), 360, false, mThickBorderPaint);
    }

    public void onObdDataChanged(ObdData data) {
        if (data == null) return;

        /*mSpeed = data.getInteger(ObdKey.SAE_VSS, 0);
        mEcoLevel = data.getInteger(ObdKey.USER_CALC_ECO_LEVEL, 0);
        mFuelEconomy = data.getFloat(ObdKey.TRIP_FUEL_ECONOMY, 0);

        if (data.getBoolean(ObdKey.CALC_FUEL_CUT, false)) {
            if ((mEventMask & EVENT_FUEL_CUT) != 0) {
                mFuelCutContinue = true;
            }
            mEventMask |= EVENT_FUEL_CUT;
        } else {
            mFuelCutContinue = false;
            mEventMask &= ~EVENT_FUEL_CUT;
        }
        if (data.getBoolean(ObdKey.CALC_HARSH_ACCEL, false)) {
            mEventMask |= EVENT_HARSH_ACCEL;
        } else {
            mEventMask &= ~EVENT_HARSH_ACCEL;
        }
        if (data.getBoolean(ObdKey.CALC_HARSH_BRAKE, false)) {
            mEventMask |= EVENT_HARSH_BRAKE;
        } else {
            mEventMask &= ~EVENT_HARSH_BRAKE;
        }
        if (data.getBoolean(ObdKey.USER_IDLING_STEP1, false)) {
            mEventMask |= EVENT_IDLING_STEP1;
        } else {
            mEventMask &= ~EVENT_IDLING_STEP1;
        }
        if (data.getBoolean(ObdKey.USER_IDLING_STEP2, false)) {
            mEventMask |= EVENT_IDLING_STEP2;
        } else {
            mEventMask &= ~EVENT_IDLING_STEP2;
        }
        if (data.getBoolean(ObdKey.USER_IDLING_STEP3, false)) {
            mEventMask |= EVENT_IDLING_STEP3;
        } else {
            mEventMask &= ~EVENT_IDLING_STEP3;
        }
        if (data.getBoolean(ObdKey.USER_OVERSPEED_STEP1, false)) {
            mEventMask |= EVENT_OVERSPEED_STEP1;
        } else {
            mEventMask &= ~EVENT_OVERSPEED_STEP1;
        }
        if (data.getBoolean(ObdKey.USER_OVERSPEED_STEP2, false)) {
            mEventMask |= EVENT_OVERSPEED_STEP2;
        } else {
            mEventMask &= ~EVENT_OVERSPEED_STEP2;
        }
        if (data.getBoolean(ObdKey.USER_OVERSPEED_STEP3, false)) {
            mEventMask |= EVENT_OVERSPEED_STEP3;
        } else {
            mEventMask &= ~EVENT_OVERSPEED_STEP3;
        }
        if (data.getBoolean(ObdKey.CALC_ECO_SPEED, false)) {
            if ((mEventMask & EVENT_ECO_SPEED) != 0) {
                mEcoSpeedContinue = true;
            }
            mEventMask |= EVENT_ECO_SPEED;
        } else {
            mEcoSpeedContinue = false;
            mEventMask &= ~EVENT_ECO_SPEED;
        }*/

        invalidate();
    }

    private int getStartAngle() {
        return 90;
    }

    private int getEcoColor(int ecoLevel) {
        switch (ecoLevel) {
            default:
            case 0: // gray
                return COLOR_GRAY;
            case 1: // blue
                return COLOR_BLUE;
            case 2: // yellow
                return COLOR_YELLOW;
            case 3: // orange
                return COLOR_ORANGE;
            case 4: // red
                return COLOR_RED;
        }
    }

    private float getSpeedAngle(int speed) {
        float angle = (360f * speed) / MAX_SPEED;
        if (angle > 360) angle = 360;
        return angle;
    }
}
