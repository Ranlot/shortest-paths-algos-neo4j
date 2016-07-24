package DistanceFunctions;

import Main.GeoLocation;

import java.util.function.BiFunction;
import java.util.function.Function;

public class NaiveGeometricDistance implements DistanceCalculator {

    public BiFunction<GeoLocation, GeoLocation, Double> getDistanceCalculator() {
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
