package mnefzger.de.sensorplatform.Utilities;

import java.util.ArrayList;

public class OSMRespone {
    public ArrayList<Element> elements;

    public class Element {
        public String type;
        public long id;
        public double lat;
        public double lon;
        public ArrayList<Long> nodes;

        public TagContainer tags;
    }

    public class TagContainer {
        public String name;
        public String maxspeed;
        public String highway;
    }


}


