package cam72cam.mod.energy;

import alexiil.mc.lib.attributes.Simulation;
import io.github.cottonmc.energy.api.DefaultEnergyTypes;
import io.github.cottonmc.energy.api.EnergyAttribute;

public interface IEnergy {
    static IEnergy from(EnergyAttribute internal) {
        return new IEnergy() {
            public int receive(int maxReceive, boolean simulate) {
                return internal.insertEnergy(DefaultEnergyTypes.MEDIUM_VOLTAGE, maxReceive, simulate ? Simulation.SIMULATE : Simulation.ACTION);
            }

            @Override
            public int extract(int maxExtract, boolean simulate) {
                return internal.extractEnergy(DefaultEnergyTypes.MEDIUM_VOLTAGE, maxExtract, simulate ? Simulation.SIMULATE : Simulation.ACTION);
            }

            @Override
            public int getCurrent() {
                return internal.getCurrentEnergy();
            }

            @Override
            public int getMax() {
                return internal.getMaxEnergy();
            }
        };
    }

    int receive(int maxReceive, boolean simulate);

    int extract(int maxExtract, boolean simulate);

    int getCurrent();

    int getMax();
}
