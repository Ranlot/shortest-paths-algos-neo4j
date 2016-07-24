package FileIO;

import Main.CityConnector;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileIOutils {

    private static final String SEPARATOR = ",";

    private Stream<String> reader;

    public FileIOutils(String fileName) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(fileName)).lines().skip(1);
    }

    private CityConnector getIndividualConnection(String rawFormConnection) {
        return new CityConnector(rawFormConnection.split(SEPARATOR));
    }

    public List<CityConnector> getAllConnections() {
        return reader.map(this::getIndividualConnection).collect(Collectors.toList());
    }


}
