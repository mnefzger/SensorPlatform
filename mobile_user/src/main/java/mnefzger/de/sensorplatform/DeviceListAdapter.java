package mnefzger.de.sensorplatform;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater inflater;
    private Context context;
    private ArrayList<BluetoothDevice> data;

    public DeviceListAdapter(Context context){
        super(context,0);
        this.inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        this.data = new ArrayList<BluetoothDevice>();
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup){
        View v = view;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_row, null);
        }

        BluetoothDevice device = null;
        if(data.size() > 0)
            device = data.get(position);

        if(device != null){
            TextView deviceText = (TextView) v.findViewById(R.id.deviceText);
            if(device.getName() != null)
                deviceText.setText(device.getName());
            else
                deviceText.setText(device.getAddress());
        }

        return v;
    }

    public void addToDeviceList(BluetoothDevice d) {
        data.add(d);
    }

    public boolean isAlreadyDiscovered(BluetoothDevice d) {
        return data.contains(d);
    }


}
