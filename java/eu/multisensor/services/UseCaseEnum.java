package eu.multisensor.services;

public enum UseCaseEnum {
    UC1_1("UC1 - Energy Policy"),
    UC1_2("UC1 - Household Appliances"),
    UC2("UC2 - Yoghurt industry");
    
    private String key;
    
    UseCaseEnum(String key) {
        this.setKey(key);
    }
    
    private void setKey(String key) {
        this.key = key;
    }
    
    public static UseCaseEnum fromString(String key) {
        if (key != null) {
            for (UseCaseEnum useCase : UseCaseEnum.values()) {
                if (key.equals(useCase.key)) {
                    return useCase;
                }
            }
        }
        throw new IllegalArgumentException("No constant with key " + key + " found");
    }
}