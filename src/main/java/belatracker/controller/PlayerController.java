package belatracker.controller;

import belatracker.model.player;
import belatracker.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("players", playerService.searchPlayers(search));
        } else {
            model.addAttribute("players", playerService.getAllPlayers());
        }
        model.addAttribute("search", search);
        return "players/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("player", new player());
        return "players/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute player player, BindingResult result) {
        if (result.hasErrors()) return "players/form";
        playerService.savePlayer(player);
        return "redirect:/players";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("player", playerService.getPlayerById(id));
        return "players/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return "redirect:/players";
    }
}