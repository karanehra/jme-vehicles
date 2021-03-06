package com.jayfella.jme.vehicle;

import com.jayfella.jme.vehicle.part.Brake;
import com.jayfella.jme.vehicle.part.Suspension;
import com.jayfella.jme.vehicle.part.Wheel;
import com.jme3.app.Application;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.List;

public class Car extends Vehicle {

    // wheel-related stuff. This isn't really "vehicle" related since a vehicle can be a boat or a helicopter.
    private final List<Wheel> wheels = new ArrayList<>();
    private TyreSmokeEmitter smokeEmitter;
    private VehicleSkidMarks skidmarks;
    private WheelSpinState wheelSpinState;


    public Car(Application app, String name) {
        super(app, name);
    }

    public int getNumWheels() {
        return wheels.size();
    }

    public Wheel getWheel(int index) {
        return wheels.get(index);
    }

    public Wheel addWheel(Spatial model, Vector3f connectionPoint, boolean isSteering, boolean steeringFlipped, Brake brake) {

        Vector3f direction = new Vector3f(0, -1, 0);
        Vector3f axle = new Vector3f(-1, 0, 0);

        float restlength = 0.2f;
        float radius = ((BoundingBox)model.getWorldBound()).getZExtent();

        // boolean isFrontWheel = false;

        VehicleWheel vehicleWheel = getVehicleControl().addWheel(
                model,
                connectionPoint,
                direction,
                axle,
                restlength,
                radius,
                isSteering);


        int index = getVehicleControl().getNumWheels() - 1;

        Suspension suspension = new Suspension(vehicleWheel, 0.2f, 0.3f);

        Wheel wheel = new Wheel(getVehicleControl(), index, isSteering, steeringFlipped, suspension, brake);

        wheels.add(wheel);
        getNode().attachChild(model);

        return wheel;
    }

    public void removeWheel(int index) {
        getVehicleControl().removeWheel(index);
        this.wheels.remove(index);
    }

    @Override
    public void accelerate(float strength) {
        super.accelerate(strength);

        if (getEngine().isStarted()) {
            for (Wheel wheel : wheels) {
                if (wheel.getAccelerationForce() > 0) {

                    float power = (getEngine().getPowerOutputAtRevs() * strength);

                    wheel.accelerate(power);
                }
                else {

                    // we always set this because the wheel could be "broken down" over time.
                    wheel.accelerate(0);
                }
            }
        }
    }

    @Override
    public void brake(float strength) {

        for (Wheel wheel : wheels) {
            wheel.brake(strength);
        }
    }

    @Override
    public void handbrake(float strength) {
        // just apply the brakes to the rear wheels.
        wheels.get(2).brake(strength, 100);
        wheels.get(3).brake(strength, 100);
    }

    @Override
    public void steer(float strength) {
        for (Wheel wheel : wheels) {
            wheel.steer(strength);
        }
    }

    public void setTyreSmokeEnabled(boolean enabled) {
        this.smokeEmitter.setEnabled(enabled);
    }

    public void setTyreSkidMarksVisible(boolean enabled) {
        this.skidmarks.setEnabled(enabled);
    }

    public void setTyreSkidMarksEnabled(boolean enabled) {
        this.skidmarks.setSkidmarkEnabled(enabled);
    }

    @Override
    public void setParkingBrakeApplied(boolean applied) {
        super.setParkingBrakeApplied(applied);

        if (applied) {
            wheels.get(2).brake(1);
            wheels.get(3).brake(1);
        }
        else {
            wheels.get(2).brake(0);
            wheels.get(3).brake(0);
        }
    }

    @Override
    public void build() {
        super.build();
        this.smokeEmitter = new TyreSmokeEmitter(this);
        this.skidmarks = new VehicleSkidMarks(
                this,
                512,
                ((BoundingBox)getWheel(0).getVehicleWheel().getWheelSpatial().getWorldBound()).getZExtent() * 0.75f);

        this.wheelSpinState = new WheelSpinState(this);
    }

    @Override
    protected void enable() {
        super.enable();

        getApplication().getStateManager().attach(smokeEmitter);
        getApplication().getStateManager().attach(skidmarks);
        getApplication().getStateManager().attach(wheelSpinState);
    }

    @Override
    protected void disable() {
        super.disable();

        getApplication().getStateManager().detach(smokeEmitter);
        getApplication().getStateManager().detach(skidmarks);
        getApplication().getStateManager().detach(wheelSpinState);
    }

    @Override
    public void applyEngineBraking() {

        for (int i = 0; i < getNumWheels(); i++) {
            Wheel wheel = getWheel(i);

            // if the wheel is not "connected" to the engine, don't slow the wheel down using engine braking.
            // so if the wheel has 1 acceleration force, apply full engine braking.
            // but if the wheel has 0 acceleration force, it's not "connected" to the engine.

            float brakingForce = getEngine().getBraking() * wheel.getAccelerationForce();
            // System.out.println(brakingForce);
            // wheel.brake(brakingForce);
            getVehicleControl().brake(i, brakingForce);
        }

    }

    @Override
    public void removeEngineBraking() {
        for (int i = 0; i < getNumWheels(); i++) {
            getVehicleControl().brake(i, 0);
        }
    }

}
