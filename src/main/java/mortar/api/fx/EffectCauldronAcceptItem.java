package mortar.api.fx;

import java.awt.Color;

import mortar.bukkit.compatibility.SoundEnum;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import mortar.api.sound.Audible;
import mortar.api.sound.Audio;
import mortar.compute.math.M;

public class EffectCauldronAcceptItem implements Effect
{
	private ParticleSpellMob effect;
	private Audible audio;

	public EffectCauldronAcceptItem()
	{
		audio = new Audio().c(SoundCategory.AMBIENT).vp(1f, 0.1f).setSound(SoundEnum.BLOCK_BREWING_STAND_BREW.bukkitSound());
		audio.addChild(((Audio) audio).clone().p(1.5f));
		effect = new ParticleSpellMob(Color.MAGENTA);
	}

	@Override
	public void play(Location l)
	{
		audio.play(l);

		for(int i = 0; i < 12; i++)
		{
			if(M.r(0.25))
			{
				effect.setColor(Color.MAGENTA.darker().darker());
			}

			if(M.r(0.25))
			{
				effect.setColor(Color.MAGENTA.brighter().brighter());
			}

			if(M.r(0.15))
			{
				effect.setColor(Color.BLUE.brighter());
			}

			effect.play(l.clone().add(0.05, 0, 0.05).add((Math.random() - 0.5) * 0.8, 0.56, (Math.random() - 0.5) * 0.8));
		}
	}

	@Override
	public void play(Player p, Location l)
	{
		audio.play(p, l);
		effect.play(l.clone().add(Math.random() / 2, 0, Math.random() / 2), p);
	}

}