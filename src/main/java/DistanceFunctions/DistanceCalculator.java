package DistanceFunctions;

import Main.GeoLocation;

import java.util.function.BiFunction;

public interface DistanceCalculator {

    BiFunction<GeoLocation, GeoLocation, Double> getDistanceCalculator(); //a function that takes 2 geolocations and returns the distance between them

}
