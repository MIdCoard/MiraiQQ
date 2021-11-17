package com.focess.core.util.option;

import com.focess.core.util.option.optiontype.OptionType;

public class OptionParserClassifier {

    private final String name;
    private final OptionType<?>[] optionTypes;

    public OptionParserClassifier(String name,OptionType<?>... optionTypes) {
        this.name = name;
        this.optionTypes = optionTypes;
    }

    public String getName() {
        return name;
    }

    public OptionType<?>[] getOptionTypes() {
        return optionTypes;
    }

    public Option createOption(String[] args) {
        if (args.length != optionTypes.length)
            return null;
        Option option = new Option(this);
        for (int i = 0;i<args.length;i++)
            if (optionTypes[i].accept(args[i]))
                option.put(optionTypes[i],args[i]);
            else return null;
        return option;
    }
}