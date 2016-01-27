package hu.kts.cmetronome;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by andrasnemeth on 25/01/16.
 */
public class WorkoutController {

    private static final String KEY_REP_COUNT = "repCount";
    private static final String KEY_SET_COUNT = "setCount";
    private static final String KEY_WORKOUT_STATUS = "workoutStatus";
    private static final String KEY_STOPWATCH_START = "stopwatchStart";

    @InjectView(R.id.rep_counter)
    TextView repCounterTextView;
    @InjectView(R.id.set_counter)
    TextView setCounterTextView;

    private Activity activity;
    int repCount = 0;
    int setCount = 0;
    private WorkoutStatus workoutStatus;


    private StopWatch stopWatch;
    private Sounds sounds;
    private IndicatorAnimation indicatorAnimation;
    private Settings settings;
    private Help help;
    private CountDowner countDowner;

    private IndicatorAnimationCallback indicatorAnimationCallback = new IndicatorAnimationCallback() {
        @Override
        public void onDownStarted() {
            sounds.makeUpSound();
        }

        @Override
        public void onRightStarted() {

        }

        @Override
        public void onUpStarted() {
            sounds.makeDownSound();
        }

        @Override
        public void onLeftStarted() {
            ++repCount;
            fillRepCounterTextViewWithTruncatedData();
        }

        @Override
        public void cycleFinished() {

        }
    };

    public WorkoutController(Activity activity, Bundle savedInstanceState) {
        this.activity = activity;
        ButterKnife.inject(this, activity);
        help = new Help(activity);
        sounds = new Sounds(activity);
        countDowner = new CountDowner(activity, this::startWorkout);
        stopWatch = new StopWatch(activity);
        settings = Settings.INSTANCE;
        initWorkoutData(savedInstanceState);
        initSettingsRelatedParts();

    }

    private void initWorkoutData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            repCount = savedInstanceState.getInt(KEY_REP_COUNT);
            setCount = savedInstanceState.getInt(KEY_SET_COUNT);
            WorkoutStatus savedWorkoutStatus = (WorkoutStatus) savedInstanceState.getSerializable(KEY_WORKOUT_STATUS);
            setWorkoutStatusAndHelpText(savedWorkoutStatus == WorkoutStatus.IN_PROGRESS || savedWorkoutStatus == WorkoutStatus.COUNTDOWN_IN_PROGRESS? WorkoutStatus.PAUSED : savedWorkoutStatus);
            if (workoutStatus == WorkoutStatus.BETWEEN_SETS) {
                stopWatch.start(savedInstanceState.getLong(KEY_STOPWATCH_START));
            }
        } else {
            setWorkoutStatusAndHelpText(WorkoutStatus.BEFORE_START);
        }
        fillRepCounterTextViewWithTruncatedData();
        fillSetCounterTextViewWithTruncatedData();
    }

    public void onRepCounterClick() {
        switch (workoutStatus) {
            case BEFORE_START:
            case PAUSED:
            case BETWEEN_SETS: countDownAndStart(); break;
            case IN_PROGRESS: pauseWorkout(); break;
        }
    }

    public boolean onRepCounterLongClick() {
        if (workoutStatus == WorkoutStatus.IN_PROGRESS || workoutStatus == WorkoutStatus.PAUSED) {
            stopSet();
            return true;
        } if (workoutStatus == WorkoutStatus.BETWEEN_SETS) {
            resetWorkout();
            return true;
        }
        return false;
    }

    private void pauseWorkout() {
        setWorkoutStatusAndHelpText(WorkoutStatus.PAUSED);
        resetIndicator();
    }

    private void resetIndicator() {
        getIndicatorAnimation().stop();
        sounds.stop();
    }


    private void countDownAndStart() {
        stopWatch.stop();
        setWorkoutStatusAndHelpText(WorkoutStatus.COUNTDOWN_IN_PROGRESS);
        countDowner.start();
    }

    private void startWorkout() {
        setWorkoutStatusAndHelpText(WorkoutStatus.IN_PROGRESS);
        fillRepCounterTextViewWithTruncatedData();
        getIndicatorAnimation().start();
    }

    /**
     * Initialization can't be called from onCreate because animation needs the actual size of the elements
     * @return
     */
    private IndicatorAnimation getIndicatorAnimation() {
        if (indicatorAnimation == null) {
            indicatorAnimation = new IndicatorAnimation(activity, indicatorAnimationCallback);
        }
        return indicatorAnimation;
    }

    private void stopSet() {
        setWorkoutStatusAndHelpText(WorkoutStatus.BETWEEN_SETS);
        resetIndicator();
        countDowner.stop();
        stopWatch.start();
        repCount = 0;
        increaseSetCounter();
    }

    private void setWorkoutStatusAndHelpText(WorkoutStatus status) {
        workoutStatus = status;
        help.setHelpTextByWorkoutStatus(status);
    }

    private void increaseSetCounter() {
        ++setCount;
        fillSetCounterTextViewWithTruncatedData();
    }


    private void resetWorkout() {
        setWorkoutStatusAndHelpText(WorkoutStatus.BEFORE_START);
        stopWatch.stop();
        resetCounters();
    }

    private void resetCounters() {
        setCount = 0;
        fillSetCounterTextViewWithTruncatedData();
        repCount = 0;
        fillSetCounterTextViewWithTruncatedData();
    }

    public void initSettingsRelatedParts() {
        help.setEnabled(settings.isShowHelp());
    }

    public void onDestroy() {
        sounds.release();
        stopWatch.stop();
        countDowner.stop();
    }

    public void saveInstanceState(Bundle outState) {
        outState.putInt(KEY_REP_COUNT, repCount);
        outState.putInt(KEY_SET_COUNT, setCount);
        outState.putSerializable(KEY_WORKOUT_STATUS, workoutStatus);
        outState.putLong(KEY_STOPWATCH_START, stopWatch.getStartTime());
    }

    /**
     * counter should contain maximum 2 digits
     */
    private void fillRepCounterTextViewWithTruncatedData() {
        repCounterTextView.setText(String.valueOf(repCount % 100));
    }

    /**
     * counter should contain maximum 2 digits
     */
    private void fillSetCounterTextViewWithTruncatedData() {
        setCounterTextView.setText(String.valueOf(setCount % 100));
    }
}