import org.neo4j.graphalgo.impl.util.GeoEstimateEvaluator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

//TODO: organise better maybe with an interface since there are 2 implementations of the distance function

public class DistanceCalculatorFactory {

    private static final String NEO4J_DISTANCE_METHOD = "distance";
    private static final String NEO4J_LATITUDE_KEY = "latKey";
    private static final String NEO4J_LONGITUDE_KEY = "lngKey";

    private final double convert2km = 1000.;

    private static GeoEstimateEvaluator geoEstimateEvaluator;
    private static Method reflectedMethod;

    private FourArgsFunction<Method, GeoEstimateEvaluator, GeoLocation, GeoLocation, Double> reflectedCalculatorfromNeo4j = (reflectedMethod, geoEstimateEvaluator, location1, location2) -> {
        return (double) reflectedMethod.invoke(geoEstimateEvaluator, location1.getLatitude(), location1.getLongitude(), location2.getLatitude(), location2.getLongitude()) / convert2km;
    };

    public DistanceCalculatorFactory() throws NoSuchMethodException {
        geoEstimateEvaluator = new GeoEstimateEvaluator(NEO4J_LATITUDE_KEY, NEO4J_LONGITUDE_KEY);
        reflectedMethod = geoEstimateEvaluator.getClass().getDeclaredMethod(NEO4J_DISTANCE_METHOD, double.class, double.class, double.class, double.class);
        reflectedMethod.setAccessible(true);
    }

    public BiFunction<GeoLocation, GeoLocation, Double> getNeo4jDistanceCalculator() {
        return (location1, location2) -> {
            try {
                //TODO: what is this return doing here?
                return reflectedCalculatorfromNeo4j.apply(reflectedMethod, geoEstimateEvaluator, location1, location2);
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        };
    }

    public BiFunction<GeoLocation, GeoLocation, Double> getNaiveDistance() {
        return (location1, location2) -> {

            Function<Double, Double> deg2rad = degree -> degree * Math.PI / 180.0;
            Function<Double, Double> rad2deg = radian -> radian * 180.0 / Math.PI;

            double theta = location1.getLongitude() - location2.getLongitude();
            double dist = Math.sin(deg2rad.apply(location1.getLatitude())) * Math.sin(deg2rad.apply(location2.getLatitude())) + Math.cos(deg2rad.apply(location1.getLatitude())) * Math.cos(deg2rad.apply(location2.getLatitude())) * Math.cos(deg2rad.apply(theta));
            dist = Math.acos(dist);
            dist = rad2deg.apply(dist);
            dist = dist * 60 * 1.1515 * 1.609344;
            return dist;
        };

    }

}

