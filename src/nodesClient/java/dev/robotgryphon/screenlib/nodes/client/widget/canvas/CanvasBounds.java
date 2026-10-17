package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

public record CanvasBounds(double minX, double minY, double maxX, double maxY) {
    public CanvasBounds union(CanvasBounds other) {
        return new CanvasBounds(Math.min(this.minX, other.minX), Math.min(this.minY, other.minY),
                Math.max(this.maxX, other.maxX), Math.max(this.maxY, other.maxY));
    }

    public CanvasBounds grow(double amount) {
        return new CanvasBounds(this.minX - amount, this.minY - amount, this.maxX + amount, this.maxY + amount);
    }

    public double width() {
        return this.maxX - this.minX;
    }

    public double height() {
        return this.maxY - this.minY;
    }

    public double centerX() {
        return (this.minX + this.maxX) / 2;
    }

    public double centerY() {
        return (this.minY + this.maxY) / 2;
    }
}
