package com.tterrag.registrate.providers;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Registry;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.HashCache;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import me.alphamode.forgetags.Tags;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jetbrains.annotations.Nullable;

public class RegistrateRecipeProvider extends FabricRecipeProvider implements RegistrateProvider, Consumer<FinishedRecipe> {
    
    private final AbstractRegistrate<?> owner;

    public RegistrateRecipeProvider(AbstractRegistrate<?> owner, FabricDataGenerator generatorIn) {
        super(generatorIn);
        this.owner = owner;
    }

    @Override
    public EnvType getSide() {
        return EnvType.SERVER;
    }
    
    @Nullable
    private Consumer<FinishedRecipe> callback;
    
    @Override
    public void accept(@Nullable FinishedRecipe t) {
        if (callback == null) {
            throw new IllegalStateException("Cannot accept recipes outside of a call to registerRecipes");
        }
        callback.accept(t);
    }

    @Override
    protected void generateRecipes(Consumer<FinishedRecipe> consumer) {
        this.callback = consumer;
        owner.genData(ProviderType.RECIPE, this);
        this.callback = null;
    }
    
    public ResourceLocation safeId(ResourceLocation id) {
        return new ResourceLocation(owner.getModid(), safeName(id));
    }

    public ResourceLocation safeId(DataIngredient source) {
        return safeId(source.getId());
    }

    public ResourceLocation safeId(ItemLike registryEntry) {
        return safeId(Registry.ITEM.getKey(registryEntry.asItem()));
    }

    public String safeName(ResourceLocation id) {
        return id.getPath().replace('/', '_');
    }

    public String safeName(DataIngredient source) {
        return safeName(source.getId());
    }

//    public String safeName(ItemLike registryEntry) {
//        return safeName(ForgeRegistries.ITEMS.getKey(registryEntry.asItem()));
//    }

    public static final int DEFAULT_SMELT_TIME = 200;
    public static final int DEFAULT_BLAST_TIME = DEFAULT_SMELT_TIME / 2;
    public static final int DEFAULT_SMOKE_TIME = DEFAULT_BLAST_TIME;
    public static final int DEFAULT_CAMPFIRE_TIME = DEFAULT_SMELT_TIME * 3;
    
    private static final String SMELTING_NAME = "smelting";
    @SuppressWarnings("null")
    private static final ImmutableMap<SimpleCookingSerializer<?>, String> COOKING_TYPE_NAMES = ImmutableMap.<SimpleCookingSerializer<?>, String>builder()
            .put(RecipeSerializer.SMELTING_RECIPE, SMELTING_NAME)
            .put(RecipeSerializer.BLASTING_RECIPE, "blasting")
            .put(RecipeSerializer.SMOKING_RECIPE, "smoking")
            .put(RecipeSerializer.CAMPFIRE_COOKING_RECIPE, "campfire")
            .build();
    
    public <T extends ItemLike> void cooking(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime, SimpleCookingSerializer<?> serializer) {
        cooking(source, result, experience, cookingTime, COOKING_TYPE_NAMES.get(serializer), serializer);
    }
    
    public <T extends ItemLike> void cooking(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime, String typeName, SimpleCookingSerializer<?> serializer) {
        SimpleCookingRecipeBuilder.cooking(source, result.get(), experience, cookingTime, serializer)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()) + "_from_" + safeName(source) + "_" + typeName);
    }
    
    public <T extends ItemLike> void smelting(DataIngredient source, Supplier<? extends T> result, float experience) {
        smelting(source, result, experience, DEFAULT_SMELT_TIME);
    }

    public <T extends ItemLike> void smelting(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime) {
        cooking(source, result, experience, cookingTime, RecipeSerializer.SMELTING_RECIPE);
    }
    
    public <T extends ItemLike> void blasting(DataIngredient source, Supplier<? extends T> result, float experience) {
        blasting(source, result, experience, DEFAULT_BLAST_TIME);
    }

    public <T extends ItemLike> void blasting(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime) {
        cooking(source, result, experience, cookingTime, RecipeSerializer.BLASTING_RECIPE);
    }

    public <T extends ItemLike> void smoking(DataIngredient source, Supplier<? extends T> result, float experience) {
        smoking(source, result, experience, DEFAULT_SMOKE_TIME);
    }

    public <T extends ItemLike> void smoking(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime) {
        cooking(source, result, experience, cookingTime, RecipeSerializer.SMOKING_RECIPE);
    }
    
    public <T extends ItemLike> void campfire(DataIngredient source, Supplier<? extends T> result, float experience) {
        campfire(source, result, experience, DEFAULT_CAMPFIRE_TIME);
    }

    public <T extends ItemLike> void campfire(DataIngredient source, Supplier<? extends T> result, float experience, int cookingTime) {
        cooking(source, result, experience, cookingTime, RecipeSerializer.CAMPFIRE_COOKING_RECIPE);
    }
    
    public <T extends ItemLike> void stonecutting(DataIngredient source, Supplier<? extends T> result) {
        stonecutting(source, result, 1);
    }

    public <T extends ItemLike> void stonecutting(DataIngredient source, Supplier<? extends T> result, int resultAmount) {
        SingleItemRecipeBuilder.stonecutting(source, result.get(), resultAmount)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()) + "_from_" + safeName(source) + "_stonecutting");
    }
    
    public <T extends ItemLike> void smeltingAndBlasting(DataIngredient source, Supplier<? extends T> result, float xp) {
        smelting(source, result, xp);
        blasting(source, result, xp);
    }

    public <T extends ItemLike> void food(DataIngredient source, Supplier<? extends T> result, float xp) {
        smelting(source, result, xp);
        smoking(source, result, xp);
        campfire(source, result, xp);
    }
    
    public <T extends ItemLike> void square(DataIngredient source, Supplier<? extends T> output, boolean small) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(output.get())
                .define('X', source);
        if (small) {
            builder.pattern("XX").pattern("XX");
        } else {
            builder.pattern("XXX").pattern("XXX").pattern("XXX");
        }
        builder.unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(output.get()));
    }

    /**
     * @param <T>
     * @param source
     * @param output
     * @deprecated Broken, use {@link #storage(NonNullSupplier, NonNullSupplier)} or {@link #storage(DataIngredient, NonNullSupplier, DataIngredient, NonNullSupplier)}.
     */
    @Deprecated
    public <T extends ItemLike> void storage(DataIngredient source, NonNullSupplier<? extends T> output) {
        square(source, output, false);
        // This is backwards, but leaving in for binary compat
        singleItemUnfinished(source, output, 1, 9)
            .save(this, safeId(source) + "_from_" + safeName(Registry.ITEM.getKey(output.get().asItem())));
    }

    public <T extends ItemLike> void storage(NonNullSupplier<? extends T> source, NonNullSupplier<? extends T> output) {
        storage(DataIngredient.items(source), source, DataIngredient.items(output), output);
    }
    
    public <T extends ItemLike> void storage(DataIngredient sourceIngredient, NonNullSupplier<? extends T> source, DataIngredient outputIngredient, NonNullSupplier<? extends T> output) {
        square(sourceIngredient, output, false);
        singleItemUnfinished(outputIngredient, source, 1, 9)
            .save(this, safeId(sourceIngredient) + "_from_" + safeName(Registry.ITEM.getKey(output.get().asItem())));
    }

//    @CheckReturnValue
    public <T extends ItemLike> ShapelessRecipeBuilder singleItemUnfinished(DataIngredient source, Supplier<? extends T> result, int required, int amount) {
        return ShapelessRecipeBuilder.shapeless(result.get(), amount)
            .requires(source, required)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this));
    }
    
    public <T extends ItemLike> void singleItem(DataIngredient source, Supplier<? extends T> result, int required, int amount) {
        singleItemUnfinished(source, result, required, amount).save(this, safeId(result.get()));
    }
    
    public <T extends ItemLike> void planks(DataIngredient source, Supplier<? extends T> result) {
        singleItemUnfinished(source, result, 1, 4)
            .group("planks")
            .save(this, safeId(result.get()));
    }
    
    public <T extends ItemLike> void stairs(DataIngredient source, Supplier<? extends T> result, @Nullable String group, boolean stone) {
        ShapedRecipeBuilder.shaped(result.get(), 4)
            .pattern("X  ").pattern("XX ").pattern("XXX")
            .define('X', source)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
        if (stone) {
            stonecutting(source, result);
        }
    }
    
    public <T extends ItemLike> void slab(DataIngredient source, Supplier<? extends T> result, @Nullable String group, boolean stone) {
        ShapedRecipeBuilder.shaped(result.get(), 6)
            .pattern("XXX")
            .define('X', source)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
        if (stone) {
            stonecutting(source, result, 2);
        }
    }
    
    public <T extends ItemLike> void fence(DataIngredient source, Supplier<? extends T> result, @Nullable String group) {
        ShapedRecipeBuilder.shaped(result.get(), 3)
            .pattern("W#W").pattern("W#W")
            .define('W', source)
            .define('#', Tags.Items.RODS_WOODEN)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
    }
    
    public <T extends ItemLike> void fenceGate(DataIngredient source, Supplier<? extends T> result, @Nullable String group) {
        ShapedRecipeBuilder.shaped(result.get())
            .pattern("#W#").pattern("#W#")
            .define('W', source)
            .define('#', Tags.Items.RODS_WOODEN)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
    }
    
    public <T extends ItemLike> void wall(DataIngredient source, Supplier<? extends T> result) {
        ShapedRecipeBuilder.shaped(result.get(), 6)
            .pattern("XXX").pattern("XXX")
            .define('X', source)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
        stonecutting(source, result);
    }
    
    public <T extends ItemLike> void door(DataIngredient source, Supplier<? extends T> result, @Nullable String group) {
        ShapedRecipeBuilder.shaped(result.get(), 3)
            .pattern("XX").pattern("XX").pattern("XX")
            .define('X', source)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
    }

    public <T extends ItemLike> void trapDoor(DataIngredient source, Supplier<? extends T> result, @Nullable String group) {
        ShapedRecipeBuilder.shaped(result.get(), 2)
            .pattern("XXX").pattern("XXX")
            .define('X', source)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCritereon(this))
            .save(this, safeId(result.get()));
    }

    // @formatter:off
    // GENERATED START

//    @Override
//    public void saveAdvancement(CachedOutput p_236368_, JsonObject p_236369_, Path p_236370_) { super.saveAdvancement(p_236368_, p_236369_, p_236370_); }

    public static void oreSmelting(Consumer<FinishedRecipe> p_176592_, List<ItemLike> p_176593_, ItemLike p_176594_, float p_176595_, int p_176596_, String p_176597_) { RecipeProvider.oreSmelting(p_176592_, p_176593_, p_176594_, p_176595_, p_176596_, p_176597_); }

    public static void oreBlasting(Consumer<FinishedRecipe> p_176626_, List<ItemLike> p_176627_, ItemLike p_176628_, float p_176629_, int p_176630_, String p_176631_) { RecipeProvider.oreBlasting(p_176626_, p_176627_, p_176628_, p_176629_, p_176630_, p_176631_); }

    public static void oreCooking(Consumer<FinishedRecipe> p_176534_, SimpleCookingSerializer<?> p_176535_, List<ItemLike> p_176536_, ItemLike p_176537_, float p_176538_, int p_176539_, String p_176540_, String p_176541_) { RecipeProvider.oreCooking(p_176534_, p_176535_, p_176536_, p_176537_, p_176538_, p_176539_, p_176540_, p_176541_); }

    public static void netheriteSmithing(Consumer<FinishedRecipe> p_125995_, Item p_125996_, Item p_125997_) { RecipeProvider.netheriteSmithing(p_125995_, p_125996_, p_125997_); }

    public static void planksFromLog(Consumer<FinishedRecipe> p_206409_, ItemLike p_206410_, TagKey<Item> p_206411_) { RecipeProvider.planksFromLog(p_206409_, p_206410_, p_206411_); }

    public static void planksFromLogs(Consumer<FinishedRecipe> p_206413_, ItemLike p_206414_, TagKey<Item> p_206415_) { RecipeProvider.planksFromLogs(p_206413_, p_206414_, p_206415_); }

    public static void woodFromLogs(Consumer<FinishedRecipe> p_126003_, ItemLike p_126004_, ItemLike p_126005_) { RecipeProvider.woodFromLogs(p_126003_, p_126004_, p_126005_); }

    public static void woodenBoat(Consumer<FinishedRecipe> p_126022_, ItemLike p_126023_, ItemLike p_126024_) { RecipeProvider.woodenBoat(p_126022_, p_126023_, p_126024_); }

    public static RecipeBuilder buttonBuilder(ItemLike p_176659_, Ingredient p_176660_) { return RecipeProvider.buttonBuilder(p_176659_, p_176660_); }

    public static RecipeBuilder doorBuilder(ItemLike p_176671_, Ingredient p_176672_) { return RecipeProvider.doorBuilder(p_176671_, p_176672_); }

    public static RecipeBuilder fenceBuilder(ItemLike p_176679_, Ingredient p_176680_) { return RecipeProvider.fenceBuilder(p_176679_, p_176680_); }

    public static RecipeBuilder fenceGateBuilder(ItemLike p_176685_, Ingredient p_176686_) { return RecipeProvider.fenceGateBuilder(p_176685_, p_176686_); }

    public static void pressurePlate(Consumer<FinishedRecipe> p_176691_, ItemLike p_176692_, ItemLike p_176693_) { RecipeProvider.pressurePlate(p_176691_, p_176692_, p_176693_); }

    public static RecipeBuilder pressurePlateBuilder(ItemLike p_176695_, Ingredient p_176696_) { return RecipeProvider.pressurePlateBuilder(p_176695_, p_176696_); }

    public static void slab(Consumer<FinishedRecipe> p_176701_, ItemLike p_176702_, ItemLike p_176703_) { RecipeProvider.slab(p_176701_, p_176702_, p_176703_); }

    public static RecipeBuilder slabBuilder(ItemLike p_176705_, Ingredient p_176706_) { return RecipeProvider.slabBuilder(p_176705_, p_176706_); }

    public static RecipeBuilder stairBuilder(ItemLike p_176711_, Ingredient p_176712_) { return RecipeProvider.stairBuilder(p_176711_, p_176712_); }

    public static RecipeBuilder trapdoorBuilder(ItemLike p_176721_, Ingredient p_176722_) { return RecipeProvider.trapdoorBuilder(p_176721_, p_176722_); }

    public static RecipeBuilder signBuilder(ItemLike p_176727_, Ingredient p_176728_) { return RecipeProvider.signBuilder(p_176727_, p_176728_); }

    public static void coloredWoolFromWhiteWoolAndDye(Consumer<FinishedRecipe> p_126062_, ItemLike p_126063_, ItemLike p_126064_) { RecipeProvider.coloredWoolFromWhiteWoolAndDye(p_126062_, p_126063_, p_126064_); }

    public static void carpet(Consumer<FinishedRecipe> p_176717_, ItemLike p_176718_, ItemLike p_176719_) { RecipeProvider.carpet(p_176717_, p_176718_, p_176719_); }

    public static void coloredCarpetFromWhiteCarpetAndDye(Consumer<FinishedRecipe> p_126070_, ItemLike p_126071_, ItemLike p_126072_) { RecipeProvider.coloredCarpetFromWhiteCarpetAndDye(p_126070_, p_126071_, p_126072_); }

    public static void bedFromPlanksAndWool(Consumer<FinishedRecipe> p_126074_, ItemLike p_126075_, ItemLike p_126076_) { RecipeProvider.bedFromPlanksAndWool(p_126074_, p_126075_, p_126076_); }

    public static void bedFromWhiteBedAndDye(Consumer<FinishedRecipe> p_126078_, ItemLike p_126079_, ItemLike p_126080_) { RecipeProvider.bedFromWhiteBedAndDye(p_126078_, p_126079_, p_126080_); }

    public static void banner(Consumer<FinishedRecipe> p_126082_, ItemLike p_126083_, ItemLike p_126084_) { RecipeProvider.banner(p_126082_, p_126083_, p_126084_); }

    public static void stainedGlassFromGlassAndDye(Consumer<FinishedRecipe> p_126086_, ItemLike p_126087_, ItemLike p_126088_) { RecipeProvider.stainedGlassFromGlassAndDye(p_126086_, p_126087_, p_126088_); }

    public static void stainedGlassPaneFromStainedGlass(Consumer<FinishedRecipe> p_126090_, ItemLike p_126091_, ItemLike p_126092_) { RecipeProvider.stainedGlassPaneFromStainedGlass(p_126090_, p_126091_, p_126092_); }

    public static void stainedGlassPaneFromGlassPaneAndDye(Consumer<FinishedRecipe> p_126094_, ItemLike p_126095_, ItemLike p_126096_) { RecipeProvider.stainedGlassPaneFromGlassPaneAndDye(p_126094_, p_126095_, p_126096_); }

    public static void coloredTerracottaFromTerracottaAndDye(Consumer<FinishedRecipe> p_126098_, ItemLike p_126099_, ItemLike p_126100_) { RecipeProvider.coloredTerracottaFromTerracottaAndDye(p_126098_, p_126099_, p_126100_); }

    public static void concretePowder(Consumer<FinishedRecipe> p_126102_, ItemLike p_126103_, ItemLike p_126104_) { RecipeProvider.concretePowder(p_126102_, p_126103_, p_126104_); }

    public static void stonecutterResultFromBase(Consumer<FinishedRecipe> p_176736_, ItemLike p_176737_, ItemLike p_176738_) { RecipeProvider.stonecutterResultFromBase(p_176736_, p_176737_, p_176738_); }

    public static void stonecutterResultFromBase(Consumer<FinishedRecipe> p_176547_, ItemLike p_176548_, ItemLike p_176549_, int p_176550_) { RecipeProvider.stonecutterResultFromBase(p_176547_, p_176548_, p_176549_, p_176550_); }

    public static void smeltingResultFromBase(Consumer<FinishedRecipe> p_176740_, ItemLike p_176741_, ItemLike p_176742_) { RecipeProvider.smeltingResultFromBase(p_176740_, p_176741_, p_176742_); }

    public static void nineBlockStorageRecipes(Consumer<FinishedRecipe> p_176744_, ItemLike p_176745_, ItemLike p_176746_) { RecipeProvider.nineBlockStorageRecipes(p_176744_, p_176745_, p_176746_); }

    public static void nineBlockStorageRecipesWithCustomPacking(Consumer<FinishedRecipe> p_176563_, ItemLike p_176564_, ItemLike p_176565_, String p_176566_, String p_176567_) { RecipeProvider.nineBlockStorageRecipesWithCustomPacking(p_176563_, p_176564_, p_176565_, p_176566_, p_176567_); }

    public static void nineBlockStorageRecipesRecipesWithCustomUnpacking(Consumer<FinishedRecipe> p_176617_, ItemLike p_176618_, ItemLike p_176619_, String p_176620_, String p_176621_) { RecipeProvider.nineBlockStorageRecipesRecipesWithCustomUnpacking(p_176617_, p_176618_, p_176619_, p_176620_, p_176621_); }

    public static void cookRecipes(Consumer<FinishedRecipe> p_126007_, String p_126008_, SimpleCookingSerializer<?> p_126009_, int p_126010_) { RecipeProvider.cookRecipes(p_126007_, p_126008_, p_126009_, p_126010_); }

    public static void simpleCookingRecipe(Consumer<FinishedRecipe> p_176584_, String p_176585_, SimpleCookingSerializer<?> p_176586_, int p_176587_, ItemLike p_176588_, ItemLike p_176589_, float p_176590_) { RecipeProvider.simpleCookingRecipe(p_176584_, p_176585_, p_176586_, p_176587_, p_176588_, p_176589_, p_176590_); }

    public static void waxRecipes(Consumer<FinishedRecipe> p_176611_) { RecipeProvider.waxRecipes(p_176611_); }

    public static void generateRecipes(Consumer<FinishedRecipe> p_176581_, BlockFamily p_176582_) { RecipeProvider.generateRecipes(p_176581_, p_176582_); }

    public static Block getBaseBlock(BlockFamily p_176524_, BlockFamily.Variant p_176525_) { return RecipeProvider.getBaseBlock(p_176524_, p_176525_); }

    public static EnterBlockTrigger.TriggerInstance insideOf(Block p_125980_) { return RecipeProvider.insideOf(p_125980_); }

    public static InventoryChangeTrigger.TriggerInstance has(MinMaxBounds.Ints p_176521_, ItemLike p_176522_) { return RecipeProvider.has(p_176521_, p_176522_); }

    public static InventoryChangeTrigger.TriggerInstance has(ItemLike p_125978_) { return RecipeProvider.has(p_125978_); }

    public static InventoryChangeTrigger.TriggerInstance has(TagKey<Item> p_206407_) { return RecipeProvider.has(p_206407_); }

    public static InventoryChangeTrigger.TriggerInstance inventoryTrigger(ItemPredicate... p_126012_) { return RecipeProvider.inventoryTrigger(p_126012_); }

    public static String getHasName(ItemLike p_176603_) { return RecipeProvider.getHasName(p_176603_); }

    public static String getItemName(ItemLike p_176633_) { return RecipeProvider.getItemName(p_176633_); }

    public static String getSimpleRecipeName(ItemLike p_176645_) { return RecipeProvider.getSimpleRecipeName(p_176645_); }

    public static String getConversionRecipeName(ItemLike p_176518_, ItemLike p_176519_) { return RecipeProvider.getConversionRecipeName(p_176518_, p_176519_); }

    public static String getSmeltingRecipeName(ItemLike p_176657_) { return RecipeProvider.getSmeltingRecipeName(p_176657_); }

    public static String getBlastingRecipeName(ItemLike p_176669_) { return RecipeProvider.getBlastingRecipeName(p_176669_); }

    // GENERATED END
}
