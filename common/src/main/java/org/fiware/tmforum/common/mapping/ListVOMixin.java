package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(as = List.class)
public abstract class ListVOMixin {
}
