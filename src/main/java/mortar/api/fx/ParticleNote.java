package mortar.api.fx;

import java.awt.Color;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import mortar.api.particle.ParticleEffect;
import mortar.api.particle.ParticleEffect.ParticleColor;

public class ParticleNote extends ParticleBase implements ColoredEffect
{
	private Color color;

	public ParticleNote(Color color)
	{
		this.color = color;
	}

	@Override
	public void play(Location l, double range)
	{
		float[] hsbVals = new float[3];
		Color.RGBtoHSB(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), hsbVals);
		ParticleColor c = new ParticleEffect.NoteColor((int) (hsbVals[0] / 24f));
		ParticleEffect.NOTE.display(c, l, range);
	}

	@Override
	public void play(Location l, Player p)
	{
		float[] hsbVals = new float[3];
		Color.RGBtoHSB(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), hsbVals);
		ParticleColor c = new ParticleEffect.NoteColor((int) (hsbVals[0] / 24f));
		ParticleEffect.NOTE.display(c, l, p);
	}

	@Override
	public ParticleNote setColor(Color color)
	{
		this.color = color;
		return this;
	}

	@Override
	public Color getColor()
	{
		return color;
	}
}
