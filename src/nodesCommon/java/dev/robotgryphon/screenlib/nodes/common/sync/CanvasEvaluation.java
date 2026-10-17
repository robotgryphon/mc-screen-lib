package dev.robotgryphon.screenlib.nodes.common.sync;


import java.util.List;

public record CanvasEvaluation(List<NodeValidity> validity, List<NodeComputed> computed,
                               List<NodeClientData> clientData) {
}
