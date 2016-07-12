package mnefzger.de.sensorplatform.Utilities;

import android.location.Location;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class OSMRespone {
    public ArrayList<Element> elements;

    class Element {
        public String type;
        public int id;
        public Location center;
        public Array nodes;
        public TagContainer tags;
    }

    class TagContainer {
        public String name;
        public String maxspeed;
    }
}


