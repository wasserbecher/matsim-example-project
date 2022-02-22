/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import com.google.inject.internal.asm.$Type;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.vehicles.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		Config config;
		/*
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		*/

		String working_directory = "scenarios/simple-2-loop-pt";

		String config_directory = working_directory + "/config.xml";

		config = ConfigUtils.loadConfig(config_directory);

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(working_directory + "/output_newLine_changedTS");

		// possibly modify config here

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

		//controler.addOverridingModule( new OTFVisLiveModule() ) ;

		createNewTrainVehicles(scenario);
		generate_new_train_line(scenario);
		controler.run();
	}

	public static void generate_new_train_line(Scenario sc){
		// fetch current transit schedule

		TransitSchedule schedule = sc.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		// create stop facilities for new line
		TransitStopFacility tsf1 = schedule.getFacilities().get(Id.get("9999", TransitStopFacility.class));
		TransitStopFacility tsf2 = schedule.getFacilities().get(Id.get("5", TransitStopFacility.class));
		TransitStopFacility tsf3 = schedule.getFacilities().get(Id.get("6", TransitStopFacility.class));

		TransitRouteStop trs1 = builder.createTransitRouteStop(tsf1, 0, 0);
		//trs1.setAwaitDepartureTime(true);
		TransitRouteStop trs2 = builder.createTransitRouteStop(tsf2, 720, 840);
		trs2.setAwaitDepartureTime(true);
		TransitRouteStop trs3 = builder.createTransitRouteStop(tsf3, 1560, 1680);
		trs3.setAwaitDepartureTime(true);

		ArrayList<TransitRouteStop> stoplist = new ArrayList<>(3);
		stoplist.add(trs1);
		stoplist.add(trs2);
		stoplist.add(trs3);

		// create network route for new line
		NetworkRoute newNetworkRoute = schedule.getTransitLines().get(Id.get("line2",TransitLine.class))
				.getRoutes().get(Id.get("line2", TransitRoute.class)).getRoute();

		// construct new route
		TransitRoute newTransitRoute = builder.createTransitRoute(Id.create("line3", TransitRoute.class),
				newNetworkRoute, stoplist,"pt");

		// construct new line
		TransitLine tLine = builder.createTransitLine(Id.create("line3", TransitLine.class));
		tLine.addRoute(newTransitRoute);
		schedule.addTransitLine(tLine);

		// add departures for new line

		double depTime = 6.0 * 3600 + 120;

		for (int i = 0; i < 4; i++){
			Departure dep = builder.createDeparture(Id.create(i, Departure.class), depTime);
			dep.setVehicleId(Id.create(i+3000, Vehicle.class));
			newTransitRoute.addDeparture(dep);
			depTime += 1800;
		}

	}


	public static void createNewTrainVehicles(Scenario sc){
		Vehicles vehicles = sc.getTransitVehicles();
		VehiclesFactory vb = vehicles.getFactory();

		//create four new vehicles
		//VehicleType vehicleType = vb.createVehicleType(Id.create("train", VehicleType.class));

		for (int i = 0; i < 4; i++){
			vehicles.addVehicle(vb.createVehicle(Id.create(3000+i, Vehicle.class), vehicles.getVehicleTypes().get(Id.get("1", VehicleType.class))));
		}

	}
	
}
