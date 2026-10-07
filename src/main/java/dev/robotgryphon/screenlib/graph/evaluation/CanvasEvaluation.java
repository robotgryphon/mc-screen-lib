package dev.robotgryphon.screenlib.graph.evaluation;

import dev.robotgryphon.screenlib.graph.validation.NodeValidity;

import java.util.List;

public record CanvasEvaluation(List<NodeValidity> validity, List<NodeComputed> computed,
                               List<NodeClientData> clientData) {
}
