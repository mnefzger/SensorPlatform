package mnefzger.de.sensorplatform.Utilities;

public class WeatherResponse {
    public Query query;

    public class Query {
        public Result results;
    }

    public class Result {
        public Channel channel;
    }

    public class Channel {
        public Wind wind;
        public Atmosphere atmosphere;
        public Item item;
    }

    public class Wind {
        public double chill;
        public double direction;
        public double speed;
    }

    public class Atmosphere {
        public double humidity;
        public double pressure;
        public double visibility;
    }

    public class Item {
        public Condition condition;
    }

    public class Condition {
        public double temp;
        public String text;
    }
}
