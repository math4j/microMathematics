/*******************************************************************************
 * microMathematics Plus - Extended visual calculator
 * *****************************************************************************
 * Copyright (C) 2014-2017 Mikhail Kulesh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mkulesh.micromath.formula;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mkulesh.micromath.formula.CalculaterTask.CancelException;
import com.mkulesh.micromath.math.CalculatedValue;
import com.mkulesh.micromath.plus.R;
import com.mkulesh.micromath.widgets.CustomEditText;
import com.mkulesh.micromath.widgets.CustomTextView;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Locale;

public class FormulaTermInterval extends FormulaTerm
{
    /**
     * Supported functions
     */
    enum IntervalType
    {
        EQUIDISTANT_INTERVAL(
                R.string.formula_quidistant_interval,
                R.drawable.p_equidistant_interval,
                R.string.math_equidistant_interval);

        private final int symbolId;
        private final int imageId;
        private final int descriptionId;

        private IntervalType(int symbolId, int imageId, int descriptionId)
        {
            this.symbolId = symbolId;
            this.imageId = imageId;
            this.descriptionId = descriptionId;
        }

        public int getSymbolId()
        {
            return symbolId;
        }

        public int getImageId()
        {
            return imageId;
        }

        public int getDescriptionId()
        {
            return descriptionId;
        }
    }

    public static IntervalType getIntervalType(Context context, String s)
    {
        IntervalType retValue = null;
        for (IntervalType f : IntervalType.values())
        {
            if (s.equals(f.toString().toLowerCase(Locale.ENGLISH))
                    || s.contains(context.getResources().getString(f.getSymbolId())))
            {
                retValue = f;
                break;
            }
        }
        return retValue;
    }

    public static boolean isInterval(Context context, String s)
    {
        return getIntervalType(context, s) != null;
    }

    /**
     * Private attributes
     */
    private IntervalType intervalType = null;
    private TermField minValueTerm, nextValueTerm, maxValueTerm = null;

    // Attention: this is not thread-safety declaration!
    private final CalculatedValue minValue = new CalculatedValue(), nextValue = new CalculatedValue(),
            maxValue = new CalculatedValue();

    /*********************************************************
     * Constructors
     *********************************************************/

    public FormulaTermInterval(TermField owner, LinearLayout layout, String s, int idx) throws Exception
    {
        super(owner.getFormulaRoot(), layout, owner.termDepth);
        setParentField(owner);
        onCreate(s, idx);
    }

    /*********************************************************
     * GUI constructors to avoid lint warning
     *********************************************************/

    public FormulaTermInterval(Context context)
    {
        super();
    }

    public FormulaTermInterval(Context context, AttributeSet attrs)
    {
        super();
    }

    /*********************************************************
     * Re-implementation for methods for FormulaBase and FormulaTerm superclass's
     *********************************************************/

    @Override
    public CalculatedValue.ValueType getValue(CalculaterTask thread, CalculatedValue outValue) throws CancelException
    {
        if (getFormulaRoot() instanceof Equation)
        {
            minValue.processRealTerm(thread, minValueTerm);
            nextValue.processRealTerm(thread, nextValueTerm);
            maxValue.processRealTerm(thread, maxValueTerm);
            if (minValue.isNaN() || nextValue.isNaN() || maxValue.isNaN())
            {
                return outValue.invalidate(CalculatedValue.ErrorType.NOT_A_REAL);
            }
            final CalculatedValue calcDelta = getDelta(minValue.getReal(), nextValue.getReal(), maxValue.getReal());
            final CalculatedValue ravArg = ((Equation) getFormulaRoot()).getArgumentValue(0);
            if (calcDelta.isNaN() || ravArg.isNaN())
            {
                return outValue.invalidate(CalculatedValue.ErrorType.NOT_A_REAL);
            }
            final long idx = ravArg.getInteger();
            final int N = getNumberOfPoints(minValue.getReal(), maxValue.getReal(), calcDelta.getReal());
            if (idx == 0)
            {
                return outValue.setValue(minValue.getReal());
            }
            else if (idx == N)
            {
                return outValue.setValue(maxValue.getReal());
            }
            else if (idx > 0 && idx < N)
            {
                return outValue.setValue(minValue.getReal() + calcDelta.getReal() * (double) idx);
            }
        }
        return outValue.invalidate(CalculatedValue.ErrorType.TERM_NOT_READY);
    }

    @Override
    public DifferentiableType isDifferentiable(String var)
    {
        return DifferentiableType.NONE;
    }

    @Override
    public CalculatedValue.ValueType getDerivativeValue(String var, CalculaterTask thread, CalculatedValue outValue)
            throws CancelException
    {
        return outValue.invalidate(CalculatedValue.ErrorType.TERM_NOT_READY);
    }

    @Override
    public TermType getTermType()
    {
        return TermType.INTERVAL;
    }

    @Override
    public String getTermCode()
    {
        return getIntervalType().toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected CustomTextView initializeSymbol(CustomTextView v)
    {
        if (v.getText() != null)
        {
            String t = v.getText().toString();
            if (t.equals(getContext().getResources().getString(R.string.formula_left_bracket_key)))
            {
                v.prepare(CustomTextView.SymbolType.LEFT_SQR_BRACKET, getFormulaRoot().getFormulaList().getActivity(),
                        this);
                v.setText("."); // this text defines view width/height
            }
            else if (t.equals(getContext().getResources().getString(R.string.formula_right_bracket_key)))
            {
                v.prepare(CustomTextView.SymbolType.RIGHT_SQR_BRACKET, getFormulaRoot().getFormulaList().getActivity(),
                        this);
                v.setText("."); // this text defines view width/height
            }
            else if (t.equals(getContext().getResources().getString(R.string.formula_first_separator_key)))
            {
                v.prepare(CustomTextView.SymbolType.TEXT, getFormulaRoot().getFormulaList().getActivity(), this);
                v.setText(getContext().getResources().getString(R.string.formula_interval_first_separator));
            }
            else if (t.equals(getContext().getResources().getString(R.string.formula_second_separator_key)))
            {
                v.prepare(CustomTextView.SymbolType.TEXT, getFormulaRoot().getFormulaList().getActivity(), this);
                v.setText(getContext().getResources().getString(R.string.formula_interval_second_separator));
            }
        }
        return v;
    }

    @Override
    protected CustomEditText initializeTerm(CustomEditText v, LinearLayout l)
    {
        if (v.getText() != null)
        {
            if (v.getText().toString().equals(getContext().getResources().getString(R.string.formula_min_value_key)))
            {
                minValueTerm = addTerm(getFormulaRoot(), l, v, this, false);
                minValueTerm.bracketsType = TermField.BracketsType.NEVER;
            }
            else if (v.getText().toString()
                    .equals(getContext().getResources().getString(R.string.formula_next_value_key)))
            {
                nextValueTerm = addTerm(getFormulaRoot(), l, v, this, false);
                nextValueTerm.bracketsType = TermField.BracketsType.NEVER;
            }
            else if (v.getText().toString()
                    .equals(getContext().getResources().getString(R.string.formula_max_value_key)))
            {
                maxValueTerm = addTerm(getFormulaRoot(), l, v, this, false);
                maxValueTerm.bracketsType = TermField.BracketsType.NEVER;
            }
        }
        return v;
    }

    /*********************************************************
     * FormulaTermInterval-specific methods
     *********************************************************/

    /**
     * Procedure creates the formula layout
     */
    private void onCreate(String s, int idx) throws Exception
    {
        if (idx < 0 || idx > layout.getChildCount())
        {
            throw new Exception("cannot create FormulaFunction for invalid insertion index " + idx);
        }
        intervalType = getIntervalType(getContext(), s);
        if (intervalType == null)
        {
            throw new Exception("cannot create FormulaFunction for unknown function");
        }
        inflateElements(R.layout.formula_interval, true);
        initializeElements(idx);
        if (minValueTerm == null || nextValueTerm == null || maxValueTerm == null)
        {
            throw new Exception("cannot initialize function terms");
        }
    }

    /**
     * Returns function type
     */
    public IntervalType getIntervalType()
    {
        return intervalType;
    }

    /**
     * Procedure returns declared interval if this root formula represents an interval
     */
    public ArrayList<Double> getInterval(CalculaterTask thread) throws CancelException
    {
        minValue.processRealTerm(thread, minValueTerm);
        nextValue.processRealTerm(thread, nextValueTerm);
        maxValue.processRealTerm(thread, maxValueTerm);
        if (minValue.isNaN() || nextValue.isNaN() || maxValue.isNaN())
        {
            return null;
        }
        final CalculatedValue calcDelta = getDelta(minValue.getReal(), nextValue.getReal(), maxValue.getReal());
        if (calcDelta.isNaN())
        {
            return null;
        }
        final int N = getNumberOfPoints(minValue.getReal(), maxValue.getReal(), calcDelta.getReal());
        ArrayList<Double> retValue = new ArrayList<Double>(N);
        for (int idx = 0; idx <= N; idx++)
        {
            if (thread != null)
            {
                thread.checkCancelation();
            }
            if (idx == 0)
            {
                retValue.add(minValue.getReal());
            }
            else if (idx == N)
            {
                retValue.add(maxValue.getReal());
            }
            else
            {
                retValue.add(minValue.getReal() + calcDelta.getReal() * (double) idx);
            }
        }
        return retValue;
    }

    /**
     * Procedure checks and returns delta value
     */
    private CalculatedValue getDelta(final double min, final double next, final double max)
    {
        final CalculatedValue calcVal = new CalculatedValue();
        if (next <= min || max < next)
        {
            // error: invalid boundaries
            calcVal.invalidate(CalculatedValue.ErrorType.NOT_A_NUMBER);
        }
        else
        {
            calcVal.setValue(next - min);
        }
        return calcVal;
    }

    private int getNumberOfPoints(double min, double max, double delta)
    {
        int N = (int) FastMath.ceil(((max - min) / delta));
        if (N > 0 && min + delta * (double) N > max + delta / 2)
        {
            N--;
        }
        return N;
    }
}
