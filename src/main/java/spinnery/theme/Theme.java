package spinnery.theme;

import com.google.gson.annotations.SerializedName;
import spinnery.container.common.widget.WButton;
import spinnery.container.common.widget.WDropdown;
import spinnery.container.common.widget.WDynamicText;
import spinnery.container.common.widget.WHorizontalSlider;
import spinnery.container.common.widget.WList;
import spinnery.container.common.widget.WPanel;
import spinnery.container.common.widget.WSlot;
import spinnery.container.common.widget.WStaticText;
import spinnery.container.common.widget.WToggle;
import spinnery.container.common.widget.WVerticalSlider;

public class Theme {
	@SerializedName("Identifier")
	String ID;
	@SerializedName("WStaticImage")
	transient Object WStaticImageTheme;
	@SerializedName("WDynamicImage")
	transient Object WDynamicImageTheme;
	@SerializedName("WPanel")
	private WPanel.Theme WPanelTheme;
	@SerializedName("WList")
	private WList.Theme WListTheme;
	@SerializedName("WDropdown")
	private WDropdown.Theme WDropdownTheme;
	@SerializedName("WStaticText")
	private WStaticText.Theme WStaticTextTheme;

	@SerializedName("WDynamicText")
	private WDynamicText.Theme WDynamicTextTheme;

	@SerializedName("WVerticalSlider")
	private WVerticalSlider.Theme WVerticalSliderTheme;

	@SerializedName("WHorizontalSlider")
	private WHorizontalSlider.Theme WHorizontalSliderTheme;

	@SerializedName("WToggle")
	private WToggle.Theme WToggleTheme;

	@SerializedName("WButton")
	private WButton.Theme WButtonTheme;

	@SerializedName("WSlot")
	private WSlot.Theme WSlotTheme;

	public void buildAll() {
		WPanelTheme.build();
		WListTheme.build();
		WDropdownTheme.build();


		WStaticTextTheme.build();
		WDynamicTextTheme.build();
		WVerticalSliderTheme.build();
		WHorizontalSliderTheme.build();
		WToggleTheme.build();
		WButtonTheme.build();
		WSlotTheme.build();
	}

	public String getID() {
		return ID;
	}

	public void setID(String ID) {
		this.ID = ID;
	}

	public WPanel.Theme getWPanelTheme() {
		return WPanelTheme;
	}

	public WList.Theme getWListTheme() {
		return WListTheme;
	}

	public WDropdown.Theme getWDropdownTheme() {
		return WDropdownTheme;
	}

	public Object getWStaticImageTheme() {
		return WStaticImageTheme;
	}

	public Object getWDynamicImageTheme() {
		return WDynamicImageTheme;
	}

	public WStaticText.Theme getWStaticTextTheme() {
		return WStaticTextTheme;
	}

	public WDynamicText.Theme getWDynamicTextTheme() {
		return WDynamicTextTheme;
	}

	public WVerticalSlider.Theme getWVerticalSliderTheme() {
		return WVerticalSliderTheme;
	}

	public WHorizontalSlider.Theme getWHorizontalSliderTheme() {
		return WHorizontalSliderTheme;
	}

	public WToggle.Theme getWToggleTheme() {
		return WToggleTheme;
	}

	public WButton.Theme getWButtonTheme() {
		return WButtonTheme;
	}

	public WSlot.Theme getWSlotTheme() {
		return WSlotTheme;
	}
}