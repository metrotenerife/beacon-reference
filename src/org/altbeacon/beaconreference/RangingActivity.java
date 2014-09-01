package org.altbeacon.beaconreference;

import java.util.Collection;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ListView;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

public class RangingActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private BeaconAdapter adapter;
    private ListView lv_beacons;    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ranging);

		initView();
		
        beaconManager.bind(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
	    beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(1000);
        try {
			beaconManager.updateScanPeriods();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        //beaconManager.debug = true;
    }
    
    private void initView() {
		lv_beacons = (ListView) findViewById(R.id.lv_beacons);
		adapter = new BeaconAdapter(this);
		lv_beacons.setAdapter(adapter);
		
	}
    
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
        @Override 
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
            Log.i(TAG, "RANGING");
        	logToDisplay(beacons);
        }

		@Override
		public void didRangeNearestBeaconInRegion(Beacon arg0, Region arg1) {
			// TODO Auto-generated method stub
			
		}

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("e2c56db5-dffb-48d2-b060-d0f5a71096e0"), null, null));
        } catch (RemoteException e) {   }
    }
    private void logToDisplay(final Collection<Beacon> beacons) {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActionBar().setSubtitle(
						"Found beacons: " + beacons.size());
				adapter.replaceWith(beacons);
			}
		});
    }
}
