package DistanceFunctions;

import Utils.FourArgsFunction;
import Main.GeoLocation;
import Utils.UsefulConstants;
import org.neo4j.graphalgo.impl.util.GeoEstimateEvaluator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class ReflectedNeo4jDistance implements DistanceCalculator {

    private static final String NEO4J_DISTANCE_METHOD = "distance";
    private static final String NEO4J_LATITUDE_KEY = "latKey";
    private static final String NEO4J_LONGITUDE_KEY = "lngKey";

    private static GeoEstimateEvaluator geoEstimateEvaluator;
    private static Method reflectedMethod;

    public ReflectedNeo4jDistance() throws NoSuchMethodException {
        geoEstimateEvaluator = new GeoEstimateEvaluator(NEO4J_LATITUDE_KEY, NEO4J_LONGITUDE_KEY);
        reflectedMethod = geoEstimateEvaluator.getClass().getDeclaredMethod(NEO4J_DISTANCE_METHOD, double.class, double.class, double.class, double.class);
        reflectedMethod.setAccessible(true);
    }

    private FourArgsFunction<Method, GeoEstimateEvaluator, GeoLocation, GeoLocation, Double> reflectedCalculatorfromNeo4j = (reflectedMethod, geoEstimateEvaluator, location1, location2) -> {
        return (double) reflectedMethod.invoke(geoEstimateEvaluator, location1.getLatitude(), location1.getLongitude(), location2.getLatitude(), location2.getLongitude()) / UsefulConstants.CONVERT_TO_KM;
    };

    public BiFunction<GeoLocation, GeoLocation, Double> getDistanceCalculator() {
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

}
