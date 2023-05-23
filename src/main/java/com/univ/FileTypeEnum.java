package com.univ;

/**
 * @author univ
 * date 2023/5/18
 */

public enum FileTypeEnum {

    GEO_JSON("geojson"),
    SHP("shp")
    ;

    private final String fileType;

    public String getFileType() {
        return fileType;
    }

    FileTypeEnum(String fileType) {
        this.fileType = fileType;
    }
}
