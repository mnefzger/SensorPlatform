package mnefzger.de.sensorplatform.Utilities;

import android.location.Location;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class OSMRespone {
    public ArrayList<Element> elements;

    public class Element {
        public String type;
        public int id;
        public double lat;
        public double lon;
        public Array nodes;
        public TagContainer tags;
    }

    public class TagContainer {
        public String name;
        public String maxspeed;
        public String highway;
    }


}


