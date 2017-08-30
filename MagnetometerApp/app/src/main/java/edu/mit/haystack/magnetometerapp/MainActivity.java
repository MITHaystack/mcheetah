package edu.mit.haystack.magnetometerapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TooManyListenersException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_WRITE_EXTERNAL = 1;
    private static final int PERMISSION_FINE_LOCATION  = 2;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    private boolean collectingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        collectingData = false;

        // TODO fix this - only one permission is requested
        // Check if we have permission to write to external storage. If not, get permission
        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                 PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                PERMISSION_WRITE_EXTERNAL);
        }

        // Check if we have permission to get location. If not, get permission
        hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                PERMISSION_FINE_LOCATION);
        }

        // TODO handle if we don't get permission - either quit or annoy the user by asking again
        // TODO maybe annoying because the requesetPermissions call happens on a separate thread

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        CheckBox startOnBoot = (CheckBox) findViewById(R.id.checkbox_start_on_boot);
        startOnBoot.setChecked(prefs.getBoolean("boot", false));

        startOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("boot", isChecked);
                editor.apply();

                if (isChecked) {
                    ComponentName receiver = new ComponentName(getApplicationContext(), BootReceiver.class);
                    PackageManager pm = getApplicationContext().getPackageManager();

                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    ComponentName receiver = new ComponentName(getApplicationContext(), BootReceiver.class);
                    PackageManager pm = getApplicationContext().getPackageManager();

                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
        });

        // Get the data sample time, default to one second (in milliseconds)
        float dataSample = prefs.getFloat("t_dataSample", 1000);
        RadioButton checkedButton;
        if (dataSample == 1000*60) {
            checkedButton = (RadioButton) findViewById(R.id.radio_sixty_seconds_data);
        } else if (dataSample == 1000*30) {
            checkedButton = (RadioButton) findViewById(R.id.radio_thirty_seconds_data);
        } else if (dataSample == 1000*10) {
            checkedButton = (RadioButton) findViewById(R.id.radio_ten_seconds_data);
        } else {
            checkedButton = (RadioButton) findViewById(R.id.radio_one_second_data);
        }
        checkedButton.setChecked(true);

        // Now do the same for GPS but default to ten seconds (in milliseconds)
        float gpsSample = prefs.getFloat("t_gpsSample", 1000*10);
        if (gpsSample == 1000*60) {
            checkedButton = (RadioButton) findViewById(R.id.radio_sixty_seconds_gps);
        } else if (gpsSample == 1000*30) {
            checkedButton = (RadioButton) findViewById(R.id.radio_thirty_seconds_gps);
        } else if (gpsSample == 1000*10) {
            checkedButton = (RadioButton) findViewById(R.id.radio_ten_seconds_gps);
        } else {
            checkedButton = (RadioButton) findViewById(R.id.radio_one_second_gps);
        }
        checkedButton.setChecked(true);

        RadioGroup groupDataSample = (RadioGroup) findViewById(R.id.group_data_sample);
        groupDataSample.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                float time;
                if (checkedId == R.id.radio_sixty_seconds_data) {
                    time = 60 * 1000;
                } else if (checkedId == R.id.radio_thirty_seconds_data) {
                    time = 30 * 1000;
                } else if (checkedId == R.id.radio_ten_seconds_data) {
                    time = 10 * 1000;
                } else {
                    time = 1000;
                }

                editor.putFloat("t_dataSample", time);
                editor.apply();
            }
        });

        RadioGroup groupGPSSample = (RadioGroup) findViewById(R.id.group_gps_sample);
        groupGPSSample.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                float time;
                if (checkedId == R.id.radio_sixty_seconds_gps) {
                    time = 60*1000;
                } else if (checkedId == R.id.radio_thirty_seconds_gps) {
                    time = 30*1000;
                } else if (checkedId == R.id.radio_ten_seconds_gps) {
                    time = 10*1000;
                } else {
                    time = 1000;
                }

                editor.putFloat("t_gpsSample", time);
                editor.apply();
            }
        });

        Button logButton = (Button) findViewById(R.id.button_start_logging);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (collectingData) {
                    cancelAlarm();
                    ((Button) findViewById(R.id.button_start_logging)).setText(R.string.button_start_logging);
                } else {
                    setAlarmRepeating();
                    Intent dataLogger = new Intent(getApplicationContext(), LoggerService.class);
                    getApplication().startService(dataLogger);
                    ((Button) findViewById(R.id.button_start_logging)).setText(R.string.button_stop_logging);
                }
                collectingData = !collectingData;
            }
        });
    }

    private void setAlarmRepeating() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        // Set alarm to midnight
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        // Repeat the alarm once every day
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 1000*60*60*24, alarmIntent);
    }

    private void cancelAlarm() {
        if (alarmManager != null) {
            alarmManager.cancel(alarmIntent);
        }
    }
}
