package mortar.api.fx;

import java.awt.Color;

import mortar.bukkit.compatibility.SoundEnum;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import mortar.api.sched.S;
import mortar.api.sound.Audible;
import mortar.api.sound.Audio;
import mortar.compute.math.M;

public class EffectCauldronRejectRecipe implements Effect
{
	private ParticleSpellMob effect;
	private Audible audio;

	public EffectCauldronRejectRecipe()
	{
		audio = new Audio().c(SoundCategory.AMBIENT).vp(1f, 1.9f).setSound(SoundEnum.BLOCK_BREWING_STAND_BREW.bukkitSound());
		audio.addChild(((Audio) audio).clone().p(0.3f).s(SoundEnum.ZOMBIE_INFECT.bukkitSound()));
		effect = new ParticleSpellMob(Color.GREEN);
	}

	@Override
	public void play(Location l)
	{
		audio.play(l);

		new S(5)
		{
			@Override
			public void run()
			{
				for(int i = 0; i < 20; i++)
				{
					if(M.r(0.25))
					{
						effect.setColor(Color.GREEN.darker().darker());
					}

					if(M.r(0.25))
					{
						effect.setColor(Color.BLACK);
					}

					if(M.r(0.15))
					{
						effect.setColor(Color.GREEN.brighter());
					}

					effect.play(l.clone().add(0.05, 0, 0.05).add((Math.random() - 0.5) * 0.8, 0.56, (Math.random() - 0.5) * 0.8));
				}
			}
		};
	}

	@Override
	public void play(Player p, Location l)
	{
		audio.play(p, l);
		effect.play(l.clone().add(Math.random() / 2, 0, Math.random() / 2), p);
	}

}