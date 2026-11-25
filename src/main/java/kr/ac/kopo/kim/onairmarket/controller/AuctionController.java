package kr.ac.kopo.kim.onairmarket.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.ac.kopo.kim.onairmarket.domain.Member;
import kr.ac.kopo.kim.onairmarket.domain.TimedAuction;
import kr.ac.kopo.kim.onairmarket.exception.ServiceException;
import kr.ac.kopo.kim.onairmarket.service.AuctionService;
import kr.ac.kopo.kim.onairmarket.service.MemberService;
import kr.ac.kopo.kim.onairmarket.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** 경매 상세 · 입찰 · 관심 저장 · 비슷한 경매 추천 */
@Controller
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final MemberService memberService;
    private final RecommendationService recommendationService;

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, HttpServletResponse response) {
        Member me = (Member) model.getAttribute("me");   // GlobalControllerAdvice 가 넣은 로그인 회원 (비로그인 null)
        TimedAuction a;
        try {
            a = auctionService.get(id);
        } catch (ServiceException e) {
            model.addAttribute("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "not-found";
        }
        long now = System.currentTimeMillis();
        model.addAttribute("a", a);
        model.addAttribute("status", a.status(now));
        model.addAttribute("open", a.isOpen(now));
        model.addAttribute("mine", me != null && a.getOwner().getId().equals(me.getId()));
        model.addAttribute("bids", auctionService.history(id));
        model.addAttribute("bookmarked", auctionService.isBookmarked(me, id));
        model.addAttribute("similar", recommendationService.similar(id));   // TF-IDF 비슷한 경매
        return "auction-detail";
    }

    @PostMapping("/{id}/bid")
    public String bid(@PathVariable String id, @RequestParam long amount, @RequestParam long revision,
                      Principal principal, RedirectAttributes ra) {
        try {
            auctionService.bid(id, amount, revision, memberService.get(principal.getName()));
            ra.addFlashAttribute("notice", "입찰했습니다. 현재 최고 입찰자입니다.");
        } catch (ServiceException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auctions/" + id;
    }

    @PostMapping("/{id}/bookmark")
    public String bookmark(@PathVariable String id, Principal principal, RedirectAttributes ra) {
        try {
            boolean saved = auctionService.toggleBookmark(memberService.get(principal.getName()), id);
            ra.addFlashAttribute("notice", saved ? "관심 경매에 저장했습니다." : "관심 저장을 해제했습니다.");
        } catch (ServiceException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auctions/" + id;
    }
}
