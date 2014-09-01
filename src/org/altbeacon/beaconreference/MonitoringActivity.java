package org.altbeacon.beaconreference;

import java.util.Collection;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

/**
 * @author MTSA
 */
public class MonitoringActivity extends Activity implements BeaconConsumer {
	protected static final String TAG = "MonitoringActivity";
    
	private final static int MAX_PERIOD = 20000;
	private final static int DEFAULT_PERIOD = 1000;
	
	private BeaconAdapter adapter;
	private BeaconManager beaconManager;
    private Vibrator v;
    private View current_view;
    private ListView lv_beacons;
    private SeekBar sb_scan_period;
    private EditText txt_scanning_period;
    private Context c;
    private long[] vibPattern = {0, 100, 100, 100};
    //private Region REGION_0 = new Region("REGION_BEACON_0", Identifier.parse("e2c56db5-dffb-48d2-b060-d0f5a71096e0"), Identifier.parse("0"), Identifier.parse("0"));
    //private Region REGION_1 = new Region("REGION_BEACON_1", Identifier.parse("e2c56db5-dffb-48d2-b060-d0f5a71096e0"), Identifier.parse("0"), Identifier.parse("1"));
	private Region REGION_GLOBAL = new Region("REGION_GLOBAL",
			Identifier.parse("e2c56db5-dffb-48d2-b060-d0f5a71096e0"), null,
			null);
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitoring);
		verifyBluetooth();

		initView();
		
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        
        // Enable the layout to detect iBeacons.
		beaconManager.enableIBeaconsDetection(true);
        
		/* beaconManager.getBeaconParsers().add(new BeaconParser()
			.setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); */
		
	    beaconManager.bind(this);
	    
		// Setting the time interval of scanning when the application is in
		// background mode.

	    beaconManager.setBackgroundBetweenScanPeriod(1000);
	    //beaconManager.setForegroundBetweenScanPeriod(5000l);
	    try {
			beaconManager.updateScanPeriods();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private void initView() {
		current_view = (View) findViewById(R.layout.activity_monitoring);
		c = this;
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		adapter = new BeaconAdapter(this);
		lv_beacons = (ListView) findViewById(R.id.lv_beacons);
		lv_beacons.setAdapter(adapter);
		sb_scan_period = (SeekBar) findViewById(R.id.sb_scanning_period);
		txt_scanning_period = (EditText) findViewById(R.id.txt_scanning_period);
		
		sb_scan_period.setMax(MAX_PERIOD / 1000);
		
		txt_scanning_period.setText(String.valueOf(DEFAULT_PERIOD / 1000));
		sb_scan_period.setProgress(DEFAULT_PERIOD / 1000);
		
		sb_scan_period.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress();
				beaconManager.setForegroundBetweenScanPeriod(progress * 1000);
				txt_scanning_period.setText(String.valueOf(progress));

				try {
					beaconManager.updateScanPeriods();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {	}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) { }
		});
		
	}
	
	public void onRangingClicked(View view) {
		Intent myIntent = new Intent(this, RangingActivity.class);
		this.startActivity(myIntent);
	}
	public void onBackgroundClicked(View view) {
		Intent myIntent = new Intent(this, BackgroundActivity.class);
		this.startActivity(myIntent);
	}

	private void verifyBluetooth() {

		try {
			if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");			
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finish();
			            System.exit(0);					
					}					
				});
				builder.show();
			}			
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Bluetooth LE not available");			
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
		            System.exit(0);					
				}
			});
			builder.show();
		}
	}	

    @Override 
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }    
    
    private void logToDisplay(final String line) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	EditText editText = (EditText)MonitoringActivity.this
    					.findViewById(R.id.monitoringText);
       	    	editText.append(line+"\n");            	    	    		
    	    }
    	});
    }
    
    // Setting the actions to do when Beacon Service is ready.
    @Override
    public void onBeaconServiceConnect() {
        // Actions to be made when Ranging beacons.
    	beaconManager.setRangeNotifier(new RangeNotifier() {
			// This method is called periodically to return all beacons in range
			// from a certain Region.
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
					Region region) {
				runOnUiThread(new Runnable() {
	    			@Override
	    			public void run() {

	    			}
				});				
			}

			// This method is called periodically to return the nearest Beacon
			// in range from a certain Region.
			@Override
			public void didRangeNearestBeaconInRegion(final Beacon beacon,
					Region region) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.replaceSingle(beacon);
					}
				});			
			}
		});
    	
    	// Define the actions to be made when Monitoring beacons.
    	beaconManager.setMonitorNotifier(new MonitorNotifier() {
        @Override
        public void didEnterRegion(final Region region) {
        	runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				v.vibrate(100);
    				Toast.makeText(c, "We have entered in: "+ region.getUniqueId(), 0).show();
    				logToDisplay("Entered in "+ region.getUniqueId());
    				createNotification(current_view, region);
    				
    				try {
						beaconManager.startRangingBeaconsInRegion(region);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
    			}
    		});
        	
        	Log.i(TAG, "I just saw a beacon named "+ region.getUniqueId() +" for the first time!" );
        }

        @Override
        public void didExitRegion(final Region region) {
        	runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				v.vibrate(vibPattern, -1);
    				Toast.makeText(c, "We have exited from: " + region.getUniqueId(), 0).show();
    				logToDisplay("Exited from "+ region.getUniqueId());
    				try {
						beaconManager.stopRangingBeaconsInRegion(region);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
    			}
    		});
        	Log.i(TAG, "I no longer see a beacon named "+ region.getUniqueId());
        }

        @Override
        public void didDetermineStateForRegion(int state, Region region) {  }
        
        });
        
        try {
        	//beaconManager.startMonitoringBeaconsInRegion(REGION_0);
        	//beaconManager.startMonitoringBeaconsInRegion(REGION_1);
        	beaconManager.startMonitoringBeaconsInRegion(REGION_GLOBAL);
        } catch (RemoteException e) {   }
        
    }
	
    
    public void createNotification(View view, Region r) {
		// Prepare intent which is triggered if the notification is selected
		// Intent intent = new Intent(this, MonitoringActivity.class);
		// PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		// Build notification
		Notification noti = new Notification.Builder(this)
		    .setContentTitle("Nueva Región Encontrada")
		    .setContentText("Se ha entrado en la región: " + r.getUniqueId())
		    //.setContentIntent(pIntent)
		    .setSmallIcon(R.drawable.ic_launcher)
		    .setLights(1, 1000, 100)
		    .build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// hide the notification after its selected
		noti.flags |= Notification.FLAG_AUTO_CANCEL;
	    notificationManager.notify(0, noti);
	
	}

	public class NotificationReceiverActivity extends Activity {
		  @Override
		  protected void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    setContentView(R.layout.region);
		  }
	}
    
    
}
