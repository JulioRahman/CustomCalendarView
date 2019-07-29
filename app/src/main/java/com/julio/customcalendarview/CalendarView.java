package com.julio.customcalendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.icu.util.IslamicCalendar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class CalendarView extends LinearLayout
{
    // for logging
    private static final String LOGTAG = "Calendar View";

    // how many days to show, defaults to six weeks, 42 days
    private static final int DAYS_COUNT = 42;

    // default date format
    private static final String DATE_FORMAT = "MMM yyyy";

    // date format
    private String dateFormat;

    // current displayed month
    private Calendar currentDate = Calendar.getInstance();

    //event handling
    private EventHandler eventHandler = null;

    // internal components
    private LinearLayout header;
    private ImageView btnPrev;
    private ImageView btnNext;
    private TextView txtDate;
    private GridView grid;

    private String[] sMonth = {"Muharram", "Safar", "Rabiul awal", "Rabiul akhir", "Jumadil awal", "Jumadil akhir", "Rajab", "Sya'ban", "Ramadhan", "Syawal", "Dzulkaidah", "Dzulhijjah"};

    private String tmp;

    private int iMonth = currentDate.get(Calendar.MONTH);

    public CalendarView(Context context)
    {
        super(context);
    }

    public CalendarView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initControl(context, attrs);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initControl(context, attrs);
    }

    /**
     * Load control xml layout
     */
    private void initControl(Context context, AttributeSet attrs)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.control_calendar, this);

        loadDateFormat(attrs);
        assignUiElements();
        assignClickHandlers();

        updateCalendar();
    }

    private void loadDateFormat(AttributeSet attrs)
    {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.CalendarView);

        try
        {
            // try to load provided date format, and fallback to default otherwise
            dateFormat = ta.getString(R.styleable.CalendarView_dateFormat);
            if (dateFormat == null)
                dateFormat = DATE_FORMAT;
        }
        finally
        {
            ta.recycle();
        }
    }

    private void assignUiElements()
    {
        // layout is inflated, assign local variables to components
        header = (LinearLayout)findViewById(R.id.calendar_header);
        btnPrev = (ImageView)findViewById(R.id.calendar_prev_button);
        btnNext = (ImageView)findViewById(R.id.calendar_next_button);
        txtDate = (TextView)findViewById(R.id.calendar_date_display);
        grid = (GridView)findViewById(R.id.calendar_grid);
    }

    private void assignClickHandlers()
    {
        // add one month and refresh UI
        btnNext.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                currentDate.add(Calendar.MONTH, 1);
                iMonth++;
                updateCalendar();
            }
        });

        // subtract one month and refresh UI
        btnPrev.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                currentDate.add(Calendar.MONTH, -1);
                iMonth--;
                updateCalendar();
            }
        });

        // long-pressing a day
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(AdapterView<?> view, View cell, int position, long id)
            {
                // handle long-press
                if (eventHandler == null)
                    return false;

                eventHandler.onDayLongPress((Date)view.getItemAtPosition(position));
                return true;
            }
        });
    }

    /**
     * Display dates correctly in grid
     */
    public void updateCalendar()
    {
        updateCalendar(null);
    }

    /**
     * Display dates correctly in grid
     */
    public void updateCalendar(HashSet<Date> events)
    {
        ArrayList<Date> cells = new ArrayList<>();
        Calendar calendar = (Calendar)currentDate.clone();

        // determine the cell for current month's beginning
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        // move calendar backwards to the beginning of the week
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        // fill cells
        while (cells.size() < DAYS_COUNT)
        {
            cells.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // update grid
        grid.setAdapter(new CalendarAdapter(getContext(), cells, events));

        // update title
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        txtDate.setText(sdf.format(currentDate.getTime()));
    }


    private class CalendarAdapter extends ArrayAdapter<Date>
    {
        // days with events
        private HashSet<Date> eventDays;

        // for view inflation
        private LayoutInflater inflater;

        public CalendarAdapter(Context context, ArrayList<Date> days, HashSet<Date> eventDays)
        {
            super(context, R.layout.control_calendar_day, days);
            this.eventDays = eventDays;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent)
        {
            // day in question
            Date date = getItem(position);
            int day = date.getDate();
            int month = date.getMonth();
            int year = date.getYear();

            // today
            Date today = new Date();

//            Date dMonth = getItem(position);
//            dMonth.setMonth(today.getMonth() + iMonth);

            // inflate item if it does not exist yet
            if (view == null)
                view = inflater.inflate(R.layout.control_calendar_day, parent, false);

            // if this day has an event, specify event image
            view.setBackgroundResource(0);
            if (eventDays != null)
            {
                for (Date eventDate : eventDays)
                {
                    if (eventDate.getDate() == day &&
                            eventDate.getMonth() == month &&
                            eventDate.getYear() == year)
                    {
                        // mark this day for event
                        view.setBackgroundResource(R.drawable.reminder);
                        break;
                    }
                }
            }

            // clear styling
            ((TextView)view).setTypeface(null, Typeface.NORMAL);
            //((TextView)view).setTextColor(Color.BLACK);
            ((TextView)view).setText("");

            // set text
            IslamicCalendar islamicCalendar = new IslamicCalendar(date);
            ((TextView)view).setText(String.valueOf(islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH)));
            //((TextView)view).setText(String.valueOf(day));

            if (date.getDay() == 1 || date.getDay() == 4) ((TextView)view).setBackgroundColor(getResources().getColor(R.color.senin_kamis));
            if (islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH) >= 13 && islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH) <= 15) ((TextView)view).setBackgroundColor(getResources().getColor(R.color.ayyamul_bidh));
            if (islamicCalendar.get(android.icu.util.Calendar.MONTH) == 8) ((TextView)view).setBackgroundColor(getResources().getColor(R.color.ramadhan));
            if (islamicCalendar.get(android.icu.util.Calendar.MONTH) == 9 && (islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH) >= 2 && islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH) <= 7)) ((TextView)view).setBackgroundColor(getResources().getColor(R.color.syawwal));
            if (islamicCalendar.get(android.icu.util.Calendar.MONTH) == 9 && islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH) == 1) ((TextView)view).setBackgroundColor(getResources().getColor(R.color.dilarang));

            if (month != iMonth % 12)
            {
                // if this day is outside current month, ngga usah di tampilin
                ((TextView)view).setText("");
                ((TextView)view).setBackgroundColor(0);
            }
            else if (day == today.getDate() && month == today.getMonth() && year == today.getYear())
            {
                // if it is today, set it to blue/bold
                ((TextView)view).setTypeface(null, Typeface.BOLD);
                ((TextView)view).setTextColor(getResources().getColor(R.color.today));
            }

            if (date.getDay() == 0) ((TextView)view).setTextColor(getResources().getColor(R.color.ahad));

//            if (day == 15) {
//                Date datee = getItem(position);
//                IslamicCalendar islamicCalendar1 = new IslamicCalendar(datee.getYear(), datee.getMonth(), 0);
//                Calendar c = Calendar.getInstance();
//                c.setTime(datee);
//                IslamicCalendar islamicCalendar2 = new IslamicCalendar(datee.getYear(), datee.getMonth(), c.getActualMaximum(Calendar.DAY_OF_MONTH));
//
//                tmp = sMonth[islamicCalendar1.get(android.icu.util.Calendar.MONTH)] + " " +
//                        islamicCalendar1.get(android.icu.util.Calendar.YEAR) + " -\n" +
//                        sMonth[islamicCalendar2.get(android.icu.util.Calendar.MONTH)] + " " +
//                        islamicCalendar2.get(android.icu.util.Calendar.YEAR);
//
//                txtDate.setText(tmp);
//            }

            if (position == 0) tmp = sMonth[islamicCalendar.get(android.icu.util.Calendar.MONTH)] + " " + islamicCalendar.get(android.icu.util.Calendar.YEAR);
            if (position == 21) txtDate.setText(tmp + " -\n" + sMonth[islamicCalendar.get(android.icu.util.Calendar.MONTH)] + " " + islamicCalendar.get(android.icu.util.Calendar.YEAR));

            return view;
        }
    }

    /**
     * Assign event handler to be passed needed events
     */
    public void setEventHandler(EventHandler eventHandler)
    {
        this.eventHandler = eventHandler;
    }

    /**
     * This interface defines what events to be reported to
     * the outside world
     */
    public interface EventHandler
    {
        void onDayLongPress(Date date);
    }
}
