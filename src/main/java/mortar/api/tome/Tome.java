package mortar.api.tome;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import mortar.bukkit.compatibility.MaterialEnum;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaBook;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;

import mortar.api.nms.Catalyst;
import mortar.lang.collection.GList;
import mortar.lang.collection.GMap;
import org.json.JSONArray;
import org.json.JSONObject;
import mortar.logic.format.F;
import mortar.logic.io.VIO;
import mortar.util.text.Alphabet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.KeybindComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class Tome
{
	public static final char[] NUM = new char[] {'\u2780', '\u2781', '\u2782', '\u2783', '\u2784', '\u2785', '\u2786', '\u2787', '\u2788', '\u2789'};
	public static final char[] NUM_FILLED = new char[] {'\u2776', '\u2777', '\u2778', '\u2779', '\u277A', '\u277B', '\u277C', '\u277D', '\u277E', '\u277F'};
	private TomeComponentBook root;
	private boolean preprocessed;
	private GList<BaseComponent> cache;

	public Tome()
	{
		root = new TomeComponentBook();
		preprocessed = false;
		cache = new GList<>();
	}

	public Alphabet getLetter()
	{
		return Alphabet.fromChar(getName().trim().charAt(0));
	}

	public ItemStack toItemStack()
	{
		ItemStack isc = new ItemStack(MaterialEnum.WRITTEN_BOOK.bukkitMaterial());
		BookMeta bm = (BookMeta) isc.getItemMeta();
		isc.setItemMeta(export(bm));

		return isc;
	}

	public void preprocessTome()
	{
		if(preprocessed)
		{
			return;
		}

		preprocessed = true;

		int m = 0;

		GList<TomeParagraph> filterParagraphs = new GList<>();

		for(TomeComponent a : root.getComponents())
		{
			if(a instanceof TomeSection)
			{
				TomeSection section = (TomeSection) a;
				if(section.isSeparate())
				{
					section.getComponents().add(0, new TomeText("!!bp!!"));
				}

				section.getComponents().add(0, new TomeAnchor("s" + m));
				section.getComponents().add(0, new TomeHeader().add(section.getSectionName()));
				section.getComponents().add(new TomeParagraph().add("\n"));

				for(TomeComponent i : section.getComponents())
				{
					if(i instanceof TomeParagraph && !(i instanceof TomeHeader))
					{
						filterParagraphs.add((TomeParagraph) i);
					}

					for(TomeComponent j : i.getComponents())
					{
						if(j instanceof TomeParagraph && !(j instanceof TomeHeader))
						{
							filterParagraphs.add((TomeParagraph) j);
						}

						for(TomeComponent k : i.getComponents())
						{
							if(k instanceof TomeParagraph && !(k instanceof TomeHeader))
							{
								filterParagraphs.add((TomeParagraph) k);
							}
						}
					}
				}

				m++;
			}
		}

		for(TomeParagraph i : filterParagraphs)
		{
			for(int j = 0; j < i.getComponents().size(); j++)
			{
				TomeComponent c = i.getComponents().get(j);

				if(c instanceof TomeText && !(c instanceof TomeFormat))
				{
					c = new TomeFormat().add(c);
				}

				i.getComponents().set(j, c);
			}
		}
	}

	private String num(int ind)
	{
		if(NUM.length >= ind)
		{
			return NUM[ind - 1] + " ";
		}

		return ind + ". ";
	}

	public BookMeta export(BookMeta meta)
	{
		CraftMetaBook book = (CraftMetaBook) meta;
		meta.setDisplayName(getRoot().getName());
		meta.setAuthor(getRoot().getAuthor());
		meta.setGeneration(Generation.TATTERED);
		book.pages.clear();

		if(!cache.isEmpty())
		{
			Catalyst.host.add(book, cache);
			return book;
		}

		preprocessTome();
		GMap<String, Integer> anchorPages = new GMap<>();
		GMap<String, String> properties = genDefaultProperties();
		GList<String> tables = new GList<>();
		int currentPage = 0;
		int currentLine = 0;
		int maxLines = 11;
		int maxCharacters = 25;

		BaseComponent content = new TextComponent("");

		for(TomeComponent a : root.getComponents())
		{
			if(a instanceof TomeMeta)
			{
				TomeMeta tm = (TomeMeta) a;
				properties.put(tm.getProperty(), tm.getValue());
			}
		}

		filterProperties(properties);

		for(TomeComponent a : root.getComponents())
		{
			if(a instanceof TomeSection)
			{
				TomeSection section = (TomeSection) a;
				tables.add(section.getSectionName());

				for(TomeComponent b : section.getComponents())
				{
					if(b instanceof TomeHeader)
					{
						BaseComponent line = exportChildren((TomeParagraph) b);
						content.addExtra(line);
					}

					else if(b instanceof TomeKeybind)
					{
						BaseComponent line = new KeybindComponent(((TomeKeybind) b).getKeyName());
						content.addExtra(line);
					}

					else if(b instanceof TomeParagraph)
					{
						BaseComponent line = exportChildren((TomeParagraph) b);
						content.addExtra(line);
					}

					else if(b instanceof TomeAnchor)
					{
						BaseComponent line = new TextComponent("!!anchor!!:" + ((TomeAnchor) b).getAnchorName());
						content.addExtra(line);
					}

					else if(b instanceof TomeUnorederdList)
					{
						for(TomeComponent c : b.getComponents())
						{
							if(c instanceof TomeParagraph)
							{
								BaseComponent line = exportChildren((TomeParagraph) c, "\u2712");
								content.addExtra(line);
							}

							else if(c instanceof TomeAnchor)
							{

							}
						}

						content.addExtra("\n");
					}

					else if(b instanceof TomeOrderedList)
					{
						int m = 1;

						for(TomeComponent c : b.getComponents())
						{
							if(c instanceof TomeParagraph)
							{
								BaseComponent line = exportChildren((TomeParagraph) c, num(m));
								content.addExtra(line);
								m++;
							}

							else if(c instanceof TomeAnchor)
							{

							}
						}

						content.addExtra("\n");
					}
				}
			}
		}

		GList<BaseComponent> elements = new GList<>(content.getExtra() == null ? new ArrayList<BaseComponent>() : content.getExtra());
		GMap<Integer, GList<BaseComponent>> pageListing = new GMap<>();

		while(!elements.isEmpty())
		{
			BaseComponent next = elements.get(0);
			String raw = next.toPlainText();
			int nls = StringUtils.countMatches(raw, '\n');
			int lineConsumption = 0;

			if(!raw.contains("!!anchor!!"))
			{
				int length = raw.length();
				lineConsumption = (int) Math.ceil((double) length / (double) maxCharacters);
				lineConsumption += (nls - 1) >= 0 ? (nls - 1) : 0;

				if(lineConsumption + currentLine > maxLines || raw.contains("!!bp!!"))
				{
					currentPage++;
					currentLine = 0;

					if(raw.contains("!!bp!!"))
					{
						continue;
					}
				}

				else
				{

				}
			}

			if(!pageListing.containsKey(currentPage))
			{
				pageListing.put(currentPage, new GList<>());
			}

			pageListing.get(currentPage).add(next);
			elements.remove(0);
			currentLine += lineConsumption;
		}

		GList<BaseComponent> prePages = new GList<>();

		if(properties.get("frontPage").equals("true"))
		{
			prePages.add(exportFrontPage());
		}

		if(properties.get("tableOfContents").equals("true"))
		{
			prePages.addAll(exportTableOfContents(tables));
		}

		for(Integer i : pageListing.k())
		{
			BaseComponent cp = new TextComponent();
			cp.addExtra("");
			cp.getExtra().clear();
			cp.getExtra().addAll(pageListing.get(i));
			prePages.add(cp);
		}

		int pg = 1;

		for(BaseComponent i : prePages)
		{
			for(BaseComponent j : new GList<>(i.getExtra() == null ? new ArrayList<>() : i.getExtra()))
			{
				for(BaseComponent k : new GList<>(j.getExtra() == null ? new ArrayList<>() : j.getExtra()))
				{
					for(BaseComponent l : new GList<>(k.getExtra() == null ? new ArrayList<>() : k.getExtra()))
					{
						for(BaseComponent m : new GList<>(l.getExtra() == null ? new ArrayList<>() : l.getExtra()))
						{
							for(BaseComponent n : new GList<>(m.getExtra() == null ? new ArrayList<>() : k.getExtra()))
							{
								if(n instanceof TextComponent && ((TextComponent) n).getText().startsWith("!!anchor!!:"))
								{
									anchorPages.put(((TextComponent) n).getText().split(":")[1], pg);

									if(m.getExtra() == null)
									{
										m.addExtra("");
										m.getExtra().clear();
									}

									m.getExtra().remove(n);
								}
							}

							if(m instanceof TextComponent && ((TextComponent) m).getText().startsWith("!!anchor!!:"))
							{
								anchorPages.put(((TextComponent) m).getText().split(":")[1], pg);

								if(l.getExtra() == null)
								{
									l.addExtra("");
									l.getExtra().clear();
								}

								l.getExtra().remove(m);
							}
						}

						if(l instanceof TextComponent && ((TextComponent) l).getText().startsWith("!!anchor!!:"))
						{
							anchorPages.put(((TextComponent) l).getText().split(":")[1], pg);

							if(k.getExtra() == null)
							{
								k.addExtra("");
								k.getExtra().clear();
							}

							k.getExtra().remove(l);
						}
					}

					if(k instanceof TextComponent && ((TextComponent) k).getText().startsWith("!!anchor!!:"))
					{
						anchorPages.put(((TextComponent) k).getText().split(":")[1], pg);

						if(j.getExtra() == null)
						{
							j.addExtra("");
							j.getExtra().clear();
						}

						j.getExtra().remove(k);
					}
				}

				if(j instanceof TextComponent && ((TextComponent) j).getText().startsWith("!!anchor!!:"))
				{
					anchorPages.put(((TextComponent) j).getText().split(":")[1], pg);

					if(i.getExtra() == null)
					{
						i.addExtra("");
						i.getExtra().clear();
					}

					i.getExtra().remove(j);
				}
			}

			pg++;
		}

		for(BaseComponent i : prePages)
		{
			try
			{
				if(i.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
				{
					Integer nt = anchorPages.get(i.getClickEvent().getValue());

					if(nt != null)
					{
						i.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
					}

					else
					{
						i.setClickEvent(null);
					}
				}
			}

			catch(Throwable e)
			{

			}

			if(i.getExtra() == null)
			{
				i.addExtra("");
				i.getExtra().clear();
			}

			for(BaseComponent j : i.getExtra())
			{
				try
				{
					if(j.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
					{
						Integer nt = anchorPages.get(j.getClickEvent().getValue());

						if(nt != null)
						{
							j.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
						}

						else
						{
							j.setClickEvent(null);
						}
					}
				}

				catch(Throwable e)
				{

				}

				if(j.getExtra() == null)
				{
					j.addExtra("");
					j.getExtra().clear();
				}

				for(BaseComponent k : j.getExtra())
				{
					try
					{
						if(k.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
						{
							Integer nt = anchorPages.get(k.getClickEvent().getValue());

							if(nt != null)
							{
								k.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
							}

							else
							{
								k.setClickEvent(null);
							}
						}
					}

					catch(Throwable e)
					{

					}

					if(k.getExtra() == null)
					{
						k.addExtra("");
						k.getExtra().clear();
					}

					for(BaseComponent l : k.getExtra())
					{
						try
						{
							if(l.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
							{
								Integer nt = anchorPages.get(l.getClickEvent().getValue());

								if(nt != null)
								{
									l.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
								}

								else
								{
									l.setClickEvent(null);
								}
							}
						}

						catch(Throwable e)
						{

						}

						if(l.getExtra() == null)
						{
							l.addExtra("");
							l.getExtra().clear();
						}

						for(BaseComponent m : l.getExtra())
						{
							try
							{
								if(m.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
								{
									Integer nt = anchorPages.get(m.getClickEvent().getValue());

									if(nt != null)
									{
										m.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
									}

									else
									{
										m.setClickEvent(null);
									}
								}
							}

							catch(Throwable e)
							{

							}

							if(m.getExtra() == null)
							{
								m.addExtra("");
								m.getExtra().clear();
							}

							for(BaseComponent n : m.getExtra())
							{
								try
								{
									if(n.getClickEvent().getAction().equals(Action.CHANGE_PAGE))
									{
										Integer nt = anchorPages.get(n.getClickEvent().getValue());

										if(nt != null)
										{
											n.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "" + nt));
										}

										else
										{
											n.setClickEvent(null);
										}
									}
								}

								catch(Throwable e)
								{

								}
							}
						}
					}
				}
			}
		}

		try
		{
			Field f = BaseComponent.class.getDeclaredField("extra");
			f.setAccessible(true);

			for(BaseComponent i : prePages)
			{
				if(i.getExtra() != null && i.getExtra().isEmpty())
				{
					f.set(i, null);
					continue;
				}

				for(BaseComponent j : i.getExtra())
				{
					if(j.getExtra() != null && j.getExtra().isEmpty())
					{
						f.set(j, null);
						continue;
					}

					for(BaseComponent k : j.getExtra())
					{
						if(k.getExtra() != null && k.getExtra().isEmpty())
						{
							f.set(k, null);
							continue;
						}

						for(BaseComponent l : k.getExtra())
						{
							if(l.getExtra() != null && l.getExtra().isEmpty())
							{
								f.set(l, null);
								continue;
							}

							for(BaseComponent m : l.getExtra())
							{
								if(m.getExtra() != null && m.getExtra().isEmpty())
								{
									f.set(m, null);
									continue;
								}

								for(BaseComponent n : m.getExtra())
								{
									if(n.getExtra() != null && n.getExtra().isEmpty())
									{
										f.set(n, null);
										continue;
									}

									for(BaseComponent o : n.getExtra())
									{
										if(o.getExtra() != null && o.getExtra().isEmpty())
										{
											f.set(o, null);
											continue;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		catch(Throwable e)
		{
			e.printStackTrace();
		}

		cache = prePages.copy();
		Catalyst.host.add(book, prePages);

		BaseComponent b = null;
		ComponentSerializer.toString(b);

		return book;
	}

	private void filterProperties(GMap<String, String> properties)
	{
		GMap<String, String> defaults = genDefaultProperties();

		for(String ii : properties.k())
		{
			String i = ii;

			for(String j : defaults.k())
			{
				if(i.trim().equalsIgnoreCase(j) && !i.equals(j))
				{
					String v = properties.get(i);
					properties.remove(i);
					properties.put(j, v);
					i = j;
				}

				if(i.equals(j))
				{
					if((defaults.get(j).equals("true") || defaults.get(j).equals("false")) && !(i.equals("true") || i.equals("false")))
					{
						String v = properties.get(i);

						if(v.trim().equalsIgnoreCase("true") || v.trim().equalsIgnoreCase("1") || v.trim().equalsIgnoreCase("enabled") || v.trim().equalsIgnoreCase("enable") || v.trim().equalsIgnoreCase("on") || v.trim().equalsIgnoreCase("yes") || v.trim().equalsIgnoreCase("+"))
						{
							properties.put(i, "true");
						}

						else
						{
							properties.put(i, "false");
						}
					}
				}
			}
		}
	}

	private GMap<String, String> genDefaultProperties()
	{
		GMap<String, String> g = new GMap<>();
		g.put("frontPage", "true");
		g.put("tableOfContents", "true");

		return g;
	}

	private GList<BaseComponent> exportTableOfContents(GList<String> tables)
	{
		GList<BaseComponent> pg = new GList<>();

		int sec = 0;
		int toc = 0;
		BaseComponent pageBuffer = new TextComponent();
		pageBuffer.addExtra("");
		pageBuffer.getExtra().clear();
		BaseComponent t = new TextComponent("Table Of Contents");
		t.setBold(true);
		t.setUnderlined(true);
		pageBuffer.addExtra(t);
		pageBuffer.addExtra("\n\n");
		boolean ad = false;
		for(String i : tables)
		{
			ad = true;
			BaseComponent content = new TextComponent();
			BaseComponent nib = new TextComponent("\u270E ");
			BaseComponent text = new TextComponent(i);
			text.setItalic(true);
			text.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, "s" + sec));
			text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {new TextComponent("Click to see '" + i + "'.")}));
			content.addExtra(nib);
			content.addExtra(text);
			pageBuffer.addExtra(content);
			toc++;
			sec++;

			if(toc > 11)
			{
				ad = false;
				toc = 0;
				pg.add(pageBuffer);
				pageBuffer = new TextComponent();
				t = new TextComponent("Table Of Contents");
				t.setBold(true);
				t.setUnderlined(true);
				pageBuffer.addExtra(t);
				pageBuffer.addExtra("\n\n");
			}

			else
			{
				pageBuffer.addExtra("\n");
			}
		}

		if(ad)
		{
			pg.add(pageBuffer);
		}

		return pg;
	}

	private BaseComponent exportFrontPage()
	{
		TextComponent cFrontPage = new TextComponent();
		TextComponent cInd = new TextComponent(F.repeat("\n", 5));
		TextComponent cTitle = new TextComponent(getName() + "\n");
		TextComponent cBy = new TextComponent("By " + getAuthor() + "\n");
		TextComponent cVolume = new TextComponent("Vol. " + getVolume() + "\n");
		cTitle.setBold(true);
		cTitle.setUnderlined(true);
		cBy.setItalic(true);
		cVolume.setItalic(true);
		cFrontPage.addExtra(cInd);
		cFrontPage.addExtra(cTitle);
		cFrontPage.addExtra(cBy);

		if(!getVolume().isEmpty())
		{
			cFrontPage.addExtra(cVolume);
		}

		return cFrontPage;
	}

	private BaseComponent exportChildren(TomeParagraph paragraph)
	{
		return exportChildren(paragraph, "");
	}

	private BaseComponent exportChildren(TomeHover paragraph)
	{
		BaseComponent line = new TextComponent();

		for(TomeComponent i : paragraph.getComponents())
		{
			if(i instanceof TomeParagraph)
			{
				BaseComponent lx = exportChildren((TomeParagraph) i);
				line.addExtra(lx);
			}

			else if(i instanceof TomeUnorederdList)
			{
				for(TomeComponent c : i.getComponents())
				{
					if(c instanceof TomeParagraph)
					{
						BaseComponent lx = exportChildren((TomeParagraph) c, "\u2712");
						line.addExtra(lx);
					}

					else if(c instanceof TomeAnchor)
					{

					}
				}
			}

			else if(i instanceof TomeOrderedList)
			{
				int m = 1;

				for(TomeComponent c : i.getComponents())
				{
					if(c instanceof TomeParagraph)
					{
						BaseComponent lx = exportChildren((TomeParagraph) c, num(m));
						line.addExtra(lx);
						m++;
					}

					else if(c instanceof TomeAnchor)
					{

					}
				}
			}

			if(i instanceof TomeFormat)
			{
				line.addExtra(exportChildren((TomeFormat) i));
			}

			if(i instanceof TomeText)
			{
				line.addExtra(((TomeText) i).getText());
			}
		}

		return line;
	}

	private BaseComponent exportChildren(TomeParagraph paragraph, String prefixLine)
	{
		BaseComponent line = new TextComponent();

		if(paragraph instanceof TomeHeader)
		{
			line.setBold(true);
			line.setUnderlined(true);
		}

		if(!prefixLine.isEmpty())
		{
			BaseComponent x = new TextComponent(prefixLine);
			line.addExtra(x);
		}

		for(TomeComponent i : paragraph.getComponents())
		{
			if(i instanceof TomeText)
			{
				line.addExtra(((TomeText) i).getText());
			}

			else if(i instanceof TomeKeybind)
			{
				line.addExtra(new KeybindComponent(((TomeKeybind) i).getKeyName()));
			}

			else if(i instanceof TomeFormat)
			{
				line.addExtra(exportChildren((TomeFormat) i));
			}
		}

		line.addExtra("\n");

		return line;
	}

	private String splitFirst(String s, String sp)
	{
		String v = "";
		boolean f = true;

		for(String i : s.split("\\Q" + sp + "\\E"))
		{
			if(f)
			{
				f = false;
				continue;
			}

			v += sp + i;
		}

		return v.substring(sp.length());
	}

	private BaseComponent exportChildren(TomeFormat format)
	{
		TextComponent sub = new TextComponent();

		if(format.getOnClick() != null)
		{
			String command = format.getOnClick();

			if(command.startsWith("run "))
			{
				sub.setClickEvent(new ClickEvent(Action.RUN_COMMAND, splitFirst(command, " ")));
			}

			if(command.startsWith("suggest "))
			{
				sub.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, splitFirst(command, " ")));
			}

			if(command.startsWith("url "))
			{
				sub.setClickEvent(new ClickEvent(Action.OPEN_URL, splitFirst(command, " ")));
			}

			if(command.startsWith("open "))
			{
				sub.setClickEvent(new ClickEvent(Action.OPEN_FILE, splitFirst(command, " ")));
			}

			if(command.startsWith("goto "))
			{
				sub.setClickEvent(new ClickEvent(Action.CHANGE_PAGE, splitFirst(command, " ")));
			}
		}

		sub.setColor(format.getColor() != null ? ChatColor.valueOf(format.getColor().toUpperCase()) : ChatColor.BLACK);

		if(format.getFormat() != null)
		{
			sub.setBold(format.getFormat().contains("bold"));
			sub.setUnderlined(format.getFormat().contains("underline"));
			sub.setStrikethrough(format.getFormat().contains("strikethrough"));
			sub.setObfuscated(format.getFormat().contains("magic"));
			sub.setItalic(format.getFormat().contains("italic"));
		}

		for(TomeComponent i : format.getComponents())
		{
			if(i instanceof TomeHover)
			{
				sub.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {exportChildren((TomeHover) i)}));
			}

			else if(i instanceof TomeKeybind)
			{
				BaseComponent line = new KeybindComponent(((TomeKeybind) i).getKeyName());
				sub.addExtra(line);
			}

			else if(i instanceof TomeText)
			{
				sub.setText(sub.getText() + ((TomeText) i).getText());
			}
		}

		return sub;
	}

	public String getName()
	{
		return getRoot().getName();
	}

	public String getId()
	{
		return getRoot().getId();
	}

	public String getVolume()
	{
		return getRoot().getVolume();
	}

	public void setName(String name)
	{
		getRoot().setName(name);
	}

	public String getAuthor()
	{
		return getRoot().getAuthor();
	}

	public void setAuthor(String author)
	{
		getRoot().setAuthor(author);
	}

	public TomeComponentBook getRoot()
	{
		return root;
	}

	public void setRoot(TomeComponentBook root)
	{
		this.root = root;
	}

	public void load(String jarPath, Class<?> anchor) throws DocumentException, IOException
	{
		VIO.readAll(anchor.getResourceAsStream("/" + (jarPath.startsWith("/") ? jarPath.substring(1) : jarPath)));
	}

	public void load(File f) throws DocumentException, IOException
	{
		load(VIO.readAll(f));
	}

	public void load(String xmlr) throws DocumentException
	{
		StringBuilder x = new StringBuilder();

		for(String i : xmlr.split("\\Q\n\\E"))
		{
			x.append(i.trim());
		}

		String xml = x.toString();
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(xml));
		setRoot(new TomeComponentBook());
		getRoot().read(document);
	}

	public void save(File f) throws IOException
	{
		VIO.writeAll(f, save());
	}

	public String save() throws IOException
	{
		Document document = DocumentHelper.createDocument();
		getRoot().construct(document);

		StringWriter w = new StringWriter();
		document.write(w);

		return format(w.toString());
	}

	private final static String format(String input)
	{
		return format(input, 4);
	}

	private final static String format(String input, int indent)
	{
		try
		{
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			return xmlOutput.getWriter().toString();
		}

		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public JSONObject toJSON()
	{
		JSONObject j = new JSONObject();
		JSONArray a = new JSONArray();

		for(BaseComponent i : cache)
		{
			a.put(new JSONObject(ComponentSerializer.toString(i)));
		}

		j.put("pages", a);

		return j;
	}
}
