package cam72cam.mod.config;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.screen.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigGui implements IScreen {
    private final ConfigGui parent;
    private final ConfigFile.PropertyClass pc;
    List<Function<IScreenBuilder, Object>> widgets = new ArrayList<>();
    private IScreenBuilder screen;


    public ConfigGui(Class<?> ...types) {
        parent = null;
        pc = null;
        int i = -1;
        for (Class<?> type : types) {
            int finalI = i;
            ConfigFile.ConfigInstance ci = new ConfigFile.ConfigInstance(type);
            ci.read();
            widgets.add(screen -> new Button(screen, 0 - 100,  finalI * 20, 200, 20, type.getSimpleName(), (hand, button) -> {
                Minecraft.getInstance().setScreen(new ScreenBuilder(new ConfigGui(ConfigGui.this, ci.pc, ci), () -> true));
            }));
            i++;
        }
    }

    public ConfigGui(ConfigGui parent, ConfigFile.PropertyClass pc, ConfigFile.ConfigInstance ci) {
        this.parent = parent;
        this.pc = pc;

        List<Consumer<Integer>> pageEvent = new ArrayList<>();
        BiConsumer<Integer, Consumer<Boolean>> onPage = (i, fn) -> {
            pageEvent.add((page) -> fn.accept(Math.floor(i / 8.0) == page));
            fn.accept(Math.floor(i / 8.0) == 0);
        };


        final int[] i = {-1};
        for (ConfigFile.Property property : pc.properties) {
            int finalI = i[0] +1;
            int offsetI = ((i[0] +1) % 8)-1;
            if (property instanceof ConfigFile.PropertyClass) {
                widgets.add(screen -> {
                    Button btn = new Button(screen, 0 - 100, offsetI * 20, 200, 20,
                                            property.getName(),
                                            (hand, button) -> Minecraft.getInstance().setScreen(new ScreenBuilder(new ConfigGui(ConfigGui.this, (ConfigFile.PropertyClass) property, ci), () -> true)));
                    onPage.accept(finalI, btn::setVisible);
                    return btn;
                });
            }
            if (property instanceof ConfigFile.PropertyField) {
                ConfigFile.PropertyField prop = (ConfigFile.PropertyField) property;
                try {
                    ConfigFile.Range range = property.getRange();

                    if (prop.field.get(null) instanceof String) {
                        String text = (String) prop.field.get(null);
                        widgets.add(screen -> {
                            TextField tf = new TextField(screen, 1, offsetI * 20 + 1, 196, 18);
                            tf.setText(text);
                            tf.setValidator(str -> {
                                try {
                                    prop.field.set(null, str);
                                    ci.write();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            });
                            onPage.accept(finalI, tf::setVisible);
                            return tf;
                        });
                    } else if (prop.field.get(null) instanceof Boolean) {
                        Boolean val = prop.field.getBoolean(null);
                        widgets.add(screen -> {
                            Button btn = new Button(screen, -1, offsetI * 20, 200, 20, val.toString(),(hand, button) -> {
                                Boolean value = !Boolean.parseBoolean(button.getText());
                                button.setText(value.toString());
                                try {
                                    prop.field.setBoolean(null, value);
                                    ci.write();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            });
                            onPage.accept(finalI, btn::setVisible);
                            return btn;
                        });
                    } else if (prop.field.getType().isEnum()) {
                        Enum val = (Enum) prop.field.get(null);
                        Enum[] arry = (Enum[]) prop.field.getType().getEnumConstants();
                        final Enum[] curr = {val};
                        widgets.add(screen -> {
                            Button btn = new Button(screen, -1, offsetI * 20, 200, 20, val.toString(), (hand, button) -> {
                                curr[0] = arry[(curr[0].ordinal()+1) % arry.length];
                                button.setText(curr[0].toString());
                                try {
                                    prop.field.set(null, curr[0]);
                                    ci.write();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            });
                            onPage.accept(finalI, btn::setVisible);
                            return btn;
                        });
                    } else if (prop.field.get(null) instanceof Double) {
                        Double val = (Double) prop.field.get(null);
                        widgets.add(screen -> {
                            if (range == null) {
                                TextField tf = new TextField(screen, 1, offsetI * 20 + 1, 196, 18);
                                tf.setText(val.toString());
                                tf.setValidator(str -> {
                                    try {
                                        prop.field.set(null, Double.parseDouble(str));
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        return false;
                                    }
                                    return true;
                                });
                                onPage.accept(finalI, tf::setVisible);
                                return tf;
                            } else {
                                Slider s = new Slider(screen, 1, offsetI * 20 + 1, "", range.min(), range.max(),
                                                      val, true, slider -> {
                                    slider.setText(String.format("%.2f", slider.getValue()));
                                    try {
                                        prop.field.set(null, slider.getValue());
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        ModCore.catching(e);
                                    }
                                });
                                s.onSlider();
                                onPage.accept(finalI, s::setVisible);
                                return s;
                            }
                        });
                    } else if (prop.field.get(null) instanceof Float) {
                        Float val = (Float) prop.field.get(null);
                        widgets.add(screen -> {
                            if (range == null) {
                                TextField tf = new TextField(screen, 1, offsetI * 20 + 1, 196, 18);
                                tf.setText(val.toString());
                                tf.setValidator(str -> {
                                    try {
                                        prop.field.set(null, Float.parseFloat(str));
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        return false;
                                    }
                                    return true;
                                });
                                onPage.accept(finalI, tf::setVisible);
                                return tf;
                            } else {
                                Slider s = new Slider(screen, 1, offsetI * 20 + 1, "", range.min(), range.max(),
                                                      val, false, slider -> {
                                    slider.setText(String.format("%.2f", slider.getValue()));
                                    try {
                                        prop.field.set(null, (float)slider.getValue());
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        ModCore.catching(e);
                                    }
                                });
                                s.onSlider();
                                onPage.accept(finalI, s::setVisible);
                                return s;
                            }
                        });
                    } else if (prop.field.get(null) instanceof Integer) {
                        Integer val = (Integer) prop.field.get(null);
                        widgets.add(screen -> {
                            if (range == null) {
                                TextField tf = new TextField(screen, 1, offsetI * 20 + 1, 196, 18);
                                tf.setText(val.toString());
                                tf.setValidator(str -> {
                                    try {
                                        prop.field.set(null, Integer.parseInt(str));
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        return false;
                                    }
                                    return true;
                                });
                                onPage.accept(finalI, tf::setVisible);
                                return tf;
                            } else {
                                Slider s = new Slider(screen, 1, offsetI * 20 + 1, "", range.min(), range.max(),
                                                      val, true, slider -> {
                                    slider.setText(slider.getValueInt() + "");
                                    try {
                                        prop.field.set(null, slider.getValueInt());
                                        ci.write();
                                    } catch (IllegalAccessException | NumberFormatException e) {
                                        ModCore.catching(e);
                                    }
                                });
                                s.onSlider();
                                onPage.accept(finalI, s::setVisible);
                                return s;
                            }
                        });
                    } else {
                        //continue;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                widgets.add(screen -> {
                    Button btn = new Button(screen, -200, offsetI * 20, 200, 20, property.getName(), (hand, button) -> {});
                    btn.setEnabled(false);
                    btn.setTooltip(Collections.singletonList(property.getComment()));
                    onPage.accept(finalI, btn::setVisible);
                    return btn;
                });
            }

            i[0]++;
        }

        int pages = (int) Math.ceil((i[0] +1) / 8.0f);
        widgets.add(screen -> new Button(screen, 0 - 100,  150, 200, 20, "Page: 1/" + pages, (hand, button) -> {
            i[0]++;
            if (i[0] >= pages) {
                i[0] = 0;
            }
            button.setText(String.format("Page: %s/%s", i[0] +1, pages));
            pageEvent.forEach(x -> x.accept(i[0]));
        }));
    }

    @Override
    public void init(IScreenBuilder screen) {
        this.screen = screen;
        for (Function<IScreenBuilder, Object> widgetsup : widgets) {
            widgetsup.apply(screen);
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (parent != null) {
            parent.show();
        }
    }

    private void show() {
        screen.show();
    }

    @Override
    public void draw(IScreenBuilder builder) {
        ((Screen)builder).renderBackground(new MatrixStack(), 0);

        String name = "";
        ConfigGui iter = this;
        while (iter != null && iter.pc != null) {
            name = " > " + iter.pc.getName() + name;
            iter = iter.parent;
        }
        name = "Config" + name;



        builder.drawCenteredString(name, 0, -50, 0xFFFFFF);
    }
}
