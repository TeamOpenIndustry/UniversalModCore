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

    /**
     * Receive energy from some source
     * @param maxReceive Max amount that can be received
     * @param simulate Whether to actually apply the change or not
     * @return the actual amount accepted
     */
    int receive(int maxReceive, boolean simulate);

    /**
     * Extract energy from this source
     * @param maxExtract Max amount that can be extracted
     * @param simulate Whether to actually apply the change or not
     * @return the actual amount extracted
     */
    int extract(int maxExtract, boolean simulate);

    /**
     * @return Current amount of energy stored
     */
    int getCurrent();

    /**
     * @return Max amount of energy that could be stored
     */
    int getMax();
}
