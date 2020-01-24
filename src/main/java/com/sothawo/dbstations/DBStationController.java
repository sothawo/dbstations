/*
 Copyright 2020 Peter-Josef Meisch (pj.meisch@sothawo.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.sothawo.dbstations;

import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dbstations")
public class DBStationController {

    private final DBStationRepository repository;

    public DBStationController(DBStationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/load")
    public void load() throws IOException, URISyntaxException {
        final List<DBStation> dbStations = Files.lines(Paths.get(getClass().getClassLoader().getResource("D_Bahnhof_2017_09.csv").toURI()))
                .skip(1)
                .map(line -> {
                    final String[] fields = line.split(";");
                    Integer id = Integer.valueOf(fields[0]);
                    String name = fields[3];
                    String type = fields[4];
                    if (type.equals("nur DPN")) {
                        type = "DPN";
                    }
                    double lon = Double.parseDouble(fields[5].replace(',', '.'));
                    double lat = Double.parseDouble(fields[6].replace(',', '.'));
                    GeoPoint location = new GeoPoint(lat, lon);
                    return new DBStation(id, name, type, location);
                })
                .collect(Collectors.toList());
        repository.saveAll(dbStations);
    }
}
